package cn.didadu.queue;

/**
 * Created by zhangjing on 16-12-23.
 */
public class SendEmailTask implements ITask{
    @Override
    public void execute(String... args) {
        for (String arg: args) {
            System.out.println("send email process running: " + arg);
        }
    }
}
