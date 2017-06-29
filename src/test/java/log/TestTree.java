package log;

import log.LoggerTest.LogItem;

import java.util.ArrayList;
import java.util.List;

import static log.Logger.Priority.name;

/**
 * Created by williamwebb on 6/28/17.
 */
public class TestTree extends Logger.DebugTree {

    static List<LogItem> items = new ArrayList<>();

    @Override protected void print(int priority, String tag, String message, Throwable throwable) {
        boolean previousWasThrowable = (items.size() > 0 && items.get(items.size() - 1).throwable != null);

        if (!message.startsWith("\t") && !previousWasThrowable) {
            items.add(new LogItem(name(priority), tag, message, null));
        } else {
            LogItem item = items.get(items.size() - 1);
            item.msg += message;
            item.throwable = null;
        }
    }
}
