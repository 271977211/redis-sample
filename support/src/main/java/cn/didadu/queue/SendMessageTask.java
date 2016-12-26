package cn.didadu.queue;

/**
 * Created by zhangjing on 16-12-23.
 */
public class SendMessageTask implements ITask{
    @Override
    public void execute(String... args) {
        for (String arg: args) {
            System.out.println("send Message process running: " + arg);
        }
    }
}
