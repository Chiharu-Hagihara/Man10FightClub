package red.man10.fightclub;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static red.man10.fightclub.FightClub.Status.*;

public final class FightClub extends JavaPlugin implements Listener {

    //   状態遷移 これらの状態遷移する
    public enum Status {
        Closed,                 //  開催前
        Entry,                  //  募集中
        Opened,                  // 予想の受付開
        Fighting,                //  対戦中
    }
    //      プレーヤ情報
    class  FighterInformation{
        String UUID;
        String name;
    }
    //      賭け情報
    class  BetInformation{
        String buyerUUID;       //  購入者のUUID
        String buyerName;       //  購入者の名前
        int    playerIndex;     //  プレーヤ情報
        double bet;             //  掛け金
    }
    double tax = 0;
    Status  currentStatus = Entry;

    //      対戦者リスト
    ArrayList<FighterInformation> filghters = new ArrayList<FighterInformation>();
    //      掛け金
    ArrayList<BetInformation> bets = new ArrayList<BetInformation>();

    //////////////////////////////////
    //    公開API
    //////////////////////////////////

    //      対戦者登録
    public int registerFighter(String uuid,String name){

        ////////////////////////////////////
        //      すでに登録されてたらエラー
        ////////////////////////////////////
        for(int i = 0;i < filghters.size();i++){
            FighterInformation player = filghters.get(i);
            if(player.UUID.equalsIgnoreCase(uuid)){
                //  登録済みエラー表示
                return -1;
            }
        }
        //      追加
        FighterInformation playerInfo = new FighterInformation();
        playerInfo.UUID = uuid;
        playerInfo.name = name;
        filghters.add(playerInfo);

        return filghters.size();
    }
    //////////////////////////////////
    //    プレーヤにかけれた金額
    //////////////////////////////////

    //
    public int getFighterIndex(String uuid) {
        for(int i = 0;i < filghters.size();i++){
            if(filghters.get(i).UUID.equalsIgnoreCase(uuid)){
                return i;
            }
        }
        return -1;
    }
    public double getFighterBetMoney(String uuid){
        int index = getFighterIndex(uuid);
        if(index == -1){
            return 0;
        }
        return getFighterBets(index);
    }
    //      購入された数
    public int getFighterBetCount(String uuid){
        int index = getFighterIndex(uuid);
        if(index == -1){
            return 0;
        }

        int count = 0;
        for(int i = 0;i < bets.size();i++){
            BetInformation bet = bets.get(i);
            if (bet.playerIndex == index){
                count ++;
            }
        }
        return count;
    }

    //////////////////////
    //      odds
    //////////////////////
    public double getFighterOdds(String uuid){

        //      購入された金額
        double bet = getFighterBetMoney(uuid);
        double total = getTotalBets();
        if(bet == 0){
            return 0;
        }
        //  （賭けられたお金の合計 － 手数料）÷【賭けに勝つ人達の勝ちに賭けた総合計金額】
        double odds = (total - tax) / bet;
        return odds;
    }

    public double getFighterBets(int playerIndex){
        double totalBet = 0;
        for(int i = 0;i < bets.size();i++){
            BetInformation bet = bets.get(i);
            if (bet.playerIndex == playerIndex){
                totalBet += bet.bet;
            }
        }
        return totalBet;
    }

    ///////////////////////////////////
    //      トータル掛け金
    ///////////////////////////////////
    public double getTotalBets(){
        double totalBet = 0;
        for(int i = 0;i < filghters.size();i++){
            totalBet = getFighterBets(i);
        }
        return totalBet;
    }
    //////////////////////////////////////////////
    //     プレイーやに賭ける 成功なら掛け金テーブルindex
    //////////////////////////////////////////////
    public int  betFighter(String fighterUUID,double price,String buyerUUID,String buyerName){

        int index = getFighterIndex(fighterUUID);
        if(index == -1){
            return -1;
        }

        BetInformation bet = new BetInformation();
        bet.bet = price;
        bet.playerIndex = index;
        bet.buyerUUID = buyerUUID;
        bet.buyerName = buyerName;
        bets.add(bet);

        return bets.size();
    }
    //////////////////////////////////////////////
    //      ゲームを中断する  払い戻し後ステータスを Closedへ
    //////////////////////////////////////////////
    public int cancelGame(){

        //   払い戻し処理


        bets.clear();
        filghters.clear();
        currentStatus = Closed;
        return 0;
    }
    //      募集開始
    public int openGame(){
        currentStatus = Opened;
        return 0;
    }
    //      ゲーム開始
    public int startGame(){
        currentStatus = Fighting;
        return 0;
    }

    //      対戦終了　winPlayer = -1 終了
    public int endGame(int winPlayer){
        if (winPlayer == -1){
            return cancelGame();
        }

        //  掛け金の計算
        double total  = getTotalBets();
        double winBet = getFighterBets(winPlayer);


        //    オッズとは
        //  （賭けられたお金の合計 － 手数料）÷【賭けに勝つ人達の勝ちに賭けた総合計金額】
        double odds = (total - tax) / winBet;

        for (int i = 0;i < bets.size();i++){
            BetInformation bet = bets.get(i);
            if (bet.playerIndex != winPlayer){
                continue;
            }
            //      プレイヤーへの支払い金額
            double playerPayout = bet.bet * odds;
            //      プレイヤーへ支払い

            //      通知
        }

        //      終了
        bets.clear();
        filghters.clear();
        currentStatus = Closed;
        return 0;
    }






    /////////////////////////////////
    //     MySQL 設定値
    /////////////////////////////////
    String  mysql_ip;
    String  mysql_port;
    String  mysql_user;
    String  mysql_pass;
    String  mysql_db;

    /////////////////////////////////
    //       設定ファイル読み込み
    /////////////////////////////////
    public void loadConfig(){
        this.reloadConfig();
        mysql_ip = this.getConfig().getString("server_config.mysql_ip");
        mysql_port = this.getConfig().getString("server_config.mysql_port");
        mysql_user = this.getConfig().getString("server_config.mysql_user");
        mysql_pass = this.getConfig().getString("server_config.mysql_pass");
        mysql_db = this.getConfig().getString("server_config.mysql_db");
        getLogger().info("Config loaded");
    }
    /////////////////////////////////
    //      起動
    /////////////////////////////////
    @Override
    public void onEnable() {
        getLogger().info("Enabled");
        this.saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents (this,this);

        //   テーブル作成
        createTables();

        //
        getCommand("mfc").setExecutor(new FightClubCommand(this));
    }

    /////////////////////////////////
    //      終了
    /////////////////////////////////
    @Override
    public void onDisable() {
        getLogger().info("Disabled");
    }

    /*
    /////////////////////////////////
    //      コマンド処理
    /////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;
        return true;
    }
*/
    /////////////////////////////////
    //     ジョインイベント
    /////////////////////////////////
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        p.sendMessage(ChatColor.YELLOW  + "Man10 Fight Club System Started.");
    }
    /////////////////////////////////
    //      チャットイベント
    /////////////////////////////////
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String message = e.getMessage();
        p.sendMessage(ChatColor.YELLOW + message );

        command("say "+message);

    }


    //////////////////////////////////////////
    //        Chatテーブル
    //////////////////////////////////////////
    String sqlCrateChatLogTable = "CREATE TABLE `mfc_chat` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `server` varchar(100) DEFAULT NULL,\n" +
            "  `name` varchar(100) DEFAULT NULL,\n" +
            "  `message` varchar(400) DEFAULT NULL,\n" +
            "  `timestamp` varchar(50) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=104377 DEFAULT CHARSET=utf8;";

    void createTables(){
        executeSQL(sqlCrateChatLogTable);
    }

    ////////////////////////////////
    //      SQL実行
    ////////////////////////////////
    Boolean executeSQL(String sql){
        // getLogger().info("executing SQL" + sql);
        Connection conn;
        try {
            //      データベース作成
            Class.forName("com.mysql.jdbc.Driver");
            String databaseURL =  "jdbc:mysql://" + mysql_ip + "/" + mysql_db ;
            //getLogger().info(databaseURL);

            conn = DriverManager.getConnection(databaseURL,mysql_user,mysql_pass);
            Statement st = conn.createStatement();
            st.execute(sql);

            st.close();
            conn.close();
            //getLogger().info("SQL performed");
            return true;
        } catch(ClassNotFoundException e){
            getLogger().warning("Could not read driver");
        } catch(SQLException e){
            getLogger().warning("Database connection error");
        }
        return false;
    }
    //      コマンド実行　
    void command(String command){
        getServer().dispatchCommand(getServer().getConsoleSender(),command);
    }
}
