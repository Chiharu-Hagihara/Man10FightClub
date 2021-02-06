package red.man10.fightclub;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static red.man10.fightclub.FightClub.autoRunnerAvailable;

public class MFCAutoRunner {
    public JavaPlugin plugin;

    public MFCAutoRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 有効にするかどうか
    public static boolean isEnabled = false;

    // スケジュール
    public static List<String> runnerSchedules = new ArrayList<>();
    // 配列化されたスケジュール
    private List<String[]> arrayedSchedules = arraySchedule();

    // 終了時のメッセージのメンバ変数
    public static String endMessage;

    // モードと開始時のメッセージのメンバ変数
    public String mode = "normal";
    public String startMessage = "&b&l&nMFCだよ！ 全員酒豪！！";

    private void commandMFC(String message, String mode) {
        Bukkit.getServer().broadcastMessage("§e============== §d●§f●§a●§e　Man10 Fight Club　§d●§f●§a● §e===============");
        Bukkit.getServer().broadcastMessage(message.replaceAll("&", "§"));
        // Bukkit.getServer().broadcastMessage("§l今日はここまで！ また次回参加してくれ！");
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mfc " + mode);
    }

    private List<String[]> arraySchedule() {
        List<String[]> arrayed = new ArrayList<>();
        for (String runnerSchedule : runnerSchedules) {
            arrayed.add(runnerSchedule.split(":"));
        }
        return arrayed;
    }

    // MFCの時間か確認
    private int isMfcTime() {
        // 配列化後用の変数
        String[] arrayedSchedule;
        for (String runnerSchedule : runnerSchedules) {
            arrayedSchedule = runnerSchedule.split(":");
            int nowDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            int nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int nowMinute = Calendar.getInstance().get(Calendar.MINUTE);
            try {
                if (Integer.parseInt(arrayedSchedule[1]) == nowHour) {
                    if (Integer.parseInt(arrayedSchedule[0]) == nowDay) {
                        int endTime = Integer.parseInt(arrayedSchedule[2]);
                        int delay = endTime * 60 - nowHour * 60 + nowMinute;
                        if (delay < 0) {
                            return -1;
                        }

                        mode = arrayedSchedule[3];
                        startMessage = arrayedSchedule[4];
                        return delay;
                    }
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public void enableAutoRunner() {
        if (!isEnabled) return;

        Calendar cal = Calendar.getInstance();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                while (autoRunnerAvailable) {
                    Thread.sleep(1000 * 60 * 10); // 10分ごとに確認
                    int time = isMfcTime();
                    if (time == -1) return; // 例外 or 今じゃない
                    if (FightClub.mode == FightClub.MFCModes.Off) { // MFCがOffか確認
                        commandMFC(startMessage, mode);
                        Thread.sleep(time * 60);
                        while (true) {
                            if (FightClub.currentStatus == FightClub.Status.Entry) { // 試合をやっているか確認
                                commandMFC(endMessage, "off");
                                return;
                            } else {
                                Thread.sleep(1000 * 10);
                            }
                        }
                    } else {
                        Thread.sleep(1000 * 60 * 60 * 5); // もし18時なってMFCが開催されていたらその日はもう開催しない
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
    }
}
