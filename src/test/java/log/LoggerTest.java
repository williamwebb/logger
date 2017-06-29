package log;

/**
 * Created by williamwebb on 6/28/17.
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LoggerTest {

    @Before @After public void setUpAndTearDown() {
        Logger.uprootAll();
        logAssert.getLogs().clear();
    }

    // NOTE: This class references the line number. Keep it at the top so it does not change.
    @Test public void debugTreeCanAlterCreatedTag() {
        Logger.plant(new TestTree() {
            @Override protected String createStackElementTag(StackTraceElement element) {
                return super.createStackElementTag(element) + ':' + element.getLineNumber();
            }
        });

        Logger.d("Test");

        assertLog()
                .hasDebugMessage("LoggerTest:35", "Test")
                .hasNoMoreMessages();
    }

    @Test public void recursion() {
        Logger.Tree timber = Logger.asTree();
        try {
            Logger.plant(timber);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Cannot plant Logger into itself.");
        }
        try {
            Logger.plant(new Logger.Tree[]{timber});
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Cannot plant Logger into itself.");
        }
    }

    @Test public void treeCount() {
        // inserts trees and checks if the amount of returned trees matches.
        assertThat(Logger.treeCount()).isEqualTo(0);
        for (int i = 1; i < 50; i++) {
            Logger.plant(new TestTree());
            assertThat(Logger.treeCount()).isEqualTo(i);
        }
        Logger.uprootAll();
        assertThat(Logger.treeCount()).isEqualTo(0);
    }

    @SuppressWarnings("ConstantConditions")
    @Test public void nullTree() {
        Logger.Tree nullTree = null;
        try {
            Logger.plant(nullTree);
            fail();
        } catch (NullPointerException e) {
            assertThat(e).hasMessage("tree == null");
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test public void nullTreeArray() {
        Logger.Tree[] nullTrees = null;
        try {
            Logger.plant(nullTrees);
            fail();
        } catch (NullPointerException e) {
            assertThat(e).hasMessage("trees == null");
        }
        nullTrees = new Logger.Tree[]{null};
        try {
            Logger.plant(nullTrees);
            fail();
        } catch (NullPointerException e) {
            assertThat(e).hasMessage("trees contains null");
        }
    }

    @Test public void forestReturnsAllPlanted() {
        TestTree tree1 = new TestTree();
        TestTree tree2 = new TestTree();
        Logger.plant(tree1);
        Logger.plant(tree2);

        assertThat(Logger.forest()).containsExactly(tree1, tree2);
    }

    @Test public void forestReturnsAllTreesPlanted() {
        TestTree tree1 = new TestTree();
        TestTree tree2 = new TestTree();
        Logger.plant(tree1, tree2);

        assertThat(Logger.forest()).containsExactly(tree1, tree2);
    }

    @Test public void uprootThrowsIfMissing() {
        try {
            Logger.uproot(new TestTree());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageStartingWith("Cannot uproot tree which is not planted: ");
        }
    }

    @Test public void uprootRemovesTree() {
        TestTree tree1 = new TestTree();
        TestTree tree2 = new TestTree();
        Logger.plant(tree1);
        Logger.plant(tree2);
        Logger.d("First");
        Logger.uproot(tree1);
        Logger.d("Second");

        assertLog()
                .hasDebugMessage("LoggerTest", "First")
                .hasDebugMessage("LoggerTest", "First")
                .hasDebugMessage("LoggerTest", "Second")
                .hasNoMoreMessages();
    }

    @Test public void uprootAllRemovesAll() {
        TestTree tree1 = new TestTree();
        TestTree tree2 = new TestTree();
        Logger.plant(tree1);
        Logger.plant(tree2);
        Logger.d("First");
        Logger.uprootAll();
        Logger.d("Second");

        assertLog()
                .hasDebugMessage("LoggerTest", "First")
                .hasDebugMessage("LoggerTest", "First")
                .hasNoMoreMessages();
    }

    @Test public void noArgsDoesNotFormat() {
        Logger.plant(new TestTree());
        Logger.d("te%st");

        assertLog()
                .hasDebugMessage("LoggerTest", "te%st")
                .hasNoMoreMessages();
    }

    @Test public void debugTreeTagGeneration() {
        Logger.plant(new TestTree());
        Logger.d("Hello, world!");

        assertLog()
                .hasDebugMessage("LoggerTest", "Hello, world!")
                .hasNoMoreMessages();
    }

    @Test public void debugTreeTagGenerationStripsAnonymousClassMarker() {
        Logger.plant(new TestTree());
        new Runnable() {
            @Override public void run() {
                Logger.d("Hello, world!");

                new Runnable() {
                    @Override public void run() {
                        Logger.d("Hello, world!");
                    }
                }.run();
            }
        }.run();

        assertLog()
                .hasDebugMessage("LoggerTest", "Hello, world!")
                .hasDebugMessage("LoggerTest", "Hello, world!")
                .hasNoMoreMessages();
    }

    @Test public void debugTreeGeneratedTagIsLoggable() {
        Logger.plant(new TestTree() {
            private static final int MAX_TAG_LENGTH = 23;

            @Override protected void log(int priority, String tag, String message, Throwable t) {
                try {
                    assertThat(tag.length() <= MAX_TAG_LENGTH);
                } catch (IllegalArgumentException e) {
                    fail(e.getMessage());
                }
                super.log(priority, tag, message, t);
            }
        });
        class ClassNameThatIsReallyReallyReallyLong {
            {
                Logger.d("Hello, world!");
            }
        }
        new ClassNameThatIsReallyReallyReallyLong();
        assertLog()
                .hasDebugMessage("LoggerTest$1ClassNameTh", "Hello, world!")
                .hasNoMoreMessages();
    }

    @Test public void debugTreeCustomTag() {
        Logger.plant(new TestTree());
        Logger.tag("Custom").d("Hello, world!");

        assertLog()
                .hasDebugMessage("Custom", "Hello, world!")
                .hasNoMoreMessages();
    }

//    @Test public void messageWithException() {
//        Logger.plant(new TestTree());
//        NullPointerException datThrowable = new NullPointerException();
//        Logger.e(datThrowable, "OMFG!");
//
//        assertExceptionLogged(Logger.Priority.ERROR, "OMFG!", "java.lang.NullPointerException");
//    }

    @Test public void exceptionOnly() {
        Logger.plant(new TestTree());

        Logger.v(new IllegalArgumentException());
        assertExceptionLogged(Logger.Priority.VERBOSE, null, "java.lang.IllegalArgumentException", "LoggerTest", 0);

        Logger.i(new NullPointerException());
        assertExceptionLogged(Logger.Priority.INFO, null, "java.lang.NullPointerException", "LoggerTest", 1);

        Logger.d(new UnsupportedOperationException());
        assertExceptionLogged(Logger.Priority.DEBUG, null, "java.lang.UnsupportedOperationException", "LoggerTest", 2);

        Logger.w(new UnknownHostException());
        assertExceptionLogged(Logger.Priority.WARN, null, "java.net.UnknownHostException", "LoggerTest", 3);

        Logger.e(new ConnectException());
        assertExceptionLogged(Logger.Priority.ERROR, null, "java.net.ConnectException", "LoggerTest", 4);

        Logger.wtf(new AssertionError());
        assertExceptionLogged(Logger.Priority.ASSERT, null, "java.lang.AssertionError", "LoggerTest", 5);
    }

    @Test public void exceptionOnlyCustomTag() {
        Logger.plant(new TestTree());

        Logger.tag("Custom").v(new IllegalArgumentException());
        assertExceptionLogged(Logger.Priority.VERBOSE, null, "java.lang.IllegalArgumentException", "Custom", 0);

        Logger.tag("Custom").i(new NullPointerException());
        assertExceptionLogged(Logger.Priority.INFO, null, "java.lang.NullPointerException", "Custom", 1);

        Logger.tag("Custom").d(new UnsupportedOperationException());
        assertExceptionLogged(Logger.Priority.DEBUG, null, "java.lang.UnsupportedOperationException", "Custom", 2);

        Logger.tag("Custom").w(new UnknownHostException());
        assertExceptionLogged(Logger.Priority.WARN, null, "java.net.UnknownHostException", "Custom", 3);

        Logger.tag("Custom").e(new ConnectException());
        assertExceptionLogged(Logger.Priority.ERROR, null, "java.net.ConnectException", "Custom", 4);

        Logger.tag("Custom").wtf(new AssertionError());
        assertExceptionLogged(Logger.Priority.ASSERT, null, "java.lang.AssertionError", "Custom", 5);
    }

//    @Test public void exceptionFromSpawnedThread() throws InterruptedException {
//        Logger.plant(new TestTree());
//        final NullPointerException datThrowable = new NullPointerException();
//        final CountDownLatch latch = new CountDownLatch(1);
//        new Thread() {
//            @Override public void run() {
//                Logger.e(datThrowable, "OMFG!");
//                latch.countDown();
//            }
//        }.start();
//        latch.await();
//        assertExceptionLogged(Logger.Priority.ERROR, "OMFG!", "java.lang.NullPointerException");
//    }

    @Test public void nullMessageWithThrowable() {
        Logger.plant(new TestTree());
        final NullPointerException datThrowable = new NullPointerException();
        Logger.e(datThrowable, null);

        assertExceptionLogged(Logger.Priority.ERROR, "", "java.lang.NullPointerException");
    }

    @Test public void chunkAcrossNewlinesAndLimit() {
        Logger.plant(new TestTree());
        Logger.d(repeat('a', 3000) + '\n' + repeat('b', 6000) + '\n' + repeat('c', 3000));

        assertLog()
                .hasDebugMessage("LoggerTest", repeat('a', 3000))
                .hasDebugMessage("LoggerTest", repeat('b', 4000))
                .hasDebugMessage("LoggerTest", repeat('b', 2000))
                .hasDebugMessage("LoggerTest", repeat('c', 3000))
                .hasNoMoreMessages();
    }

    @Test public void nullMessageWithoutThrowable() {
        Logger.plant(new TestTree());
        Logger.d(null);

        assertLog().hasNoMoreMessages();
    }

    @Test public void logMessageCallback() {
        final List<String> logs = new ArrayList<String>();
        Logger.plant(new TestTree() {
            @Override protected void log(int priority, String tag, String message, Throwable t) {
                logs.add(priority + " " + tag + " " + message);
            }
        });

        Logger.v("Verbose");
        Logger.tag("Custom").v("Verbose");
        Logger.d("Debug");
        Logger.tag("Custom").d("Debug");
        Logger.i("Info");
        Logger.tag("Custom").i("Info");
        Logger.w("Warn");
        Logger.tag("Custom").w("Warn");
        Logger.e("Error");
        Logger.tag("Custom").e("Error");
        Logger.wtf("Assert");
        Logger.tag("Custom").wtf("Assert");

        assertThat(logs).containsExactly( //
                "2 LoggerTest Verbose", //
                "2 Custom Verbose", //
                "3 LoggerTest Debug", //
                "3 Custom Debug", //
                "4 LoggerTest Info", //
                "4 Custom Info", //
                "5 LoggerTest Warn", //
                "5 Custom Warn", //
                "6 LoggerTest Error", //
                "6 Custom Error", //
                "7 LoggerTest Assert", //
                "7 Custom Assert" //
        );
    }

    @Test public void logAtSpecifiedPriority() {
        Logger.plant(new TestTree());

        Logger.log(Logger.Priority.VERBOSE, "Hello, World!");
        Logger.log(Logger.Priority.DEBUG, "Hello, World!");
        Logger.log(Logger.Priority.INFO, "Hello, World!");
        Logger.log(Logger.Priority.WARN, "Hello, World!");
        Logger.log(Logger.Priority.ERROR, "Hello, World!");
        Logger.log(Logger.Priority.ASSERT, "Hello, World!");

        assertLog()
                .hasVerboseMessage("LoggerTest", "Hello, World!")
                .hasDebugMessage("LoggerTest", "Hello, World!")
                .hasInfoMessage("LoggerTest", "Hello, World!")
                .hasWarnMessage("LoggerTest", "Hello, World!")
                .hasErrorMessage("LoggerTest", "Hello, World!")
                .hasAssertMessage("LoggerTest", "Hello, World!")
                .hasNoMoreMessages();
    }

    @Test public void formatting() {
        Logger.plant(new TestTree());
        Logger.v("Hello, %s!", "World");
        Logger.d("Hello, %s!", "World");
        Logger.i("Hello, %s!", "World");
        Logger.w("Hello, %s!", "World");
        Logger.e("Hello, %s!", "World");
        Logger.wtf("Hello, %s!", "World");

        assertLog()
                .hasVerboseMessage("LoggerTest", "Hello, World!")
                .hasDebugMessage("LoggerTest", "Hello, World!")
                .hasInfoMessage("LoggerTest", "Hello, World!")
                .hasWarnMessage("LoggerTest", "Hello, World!")
                .hasErrorMessage("LoggerTest", "Hello, World!")
                .hasAssertMessage("LoggerTest", "Hello, World!")
                .hasNoMoreMessages();
    }

    @Test public void isLoggableControlsLogging() {
        Logger.plant(new TestTree() {
            @Override protected boolean isLoggable(int priority) {
                return priority == Logger.Priority.INFO;
            }
        });
        Logger.v("Hello, World!");
        Logger.d("Hello, World!");
        Logger.i("Hello, World!");
        Logger.w("Hello, World!");
        Logger.e("Hello, World!");
        Logger.wtf("Hello, World!");

        assertLog()
                .hasInfoMessage("LoggerTest", "Hello, World!")
                .hasNoMoreMessages();
    }

    @Test public void isLoggableTagControlsLogging() {
        Logger.plant(new TestTree() {
            @Override protected boolean isLoggable(String tag, int priority) {
                return "FILTER".equals(tag);
            }
        });
        Logger.tag("FILTER").v("Hello, World!");
        Logger.d("Hello, World!");
        Logger.i("Hello, World!");
        Logger.w("Hello, World!");
        Logger.e("Hello, World!");
        Logger.wtf("Hello, World!");

        assertLog()
                .hasVerboseMessage("FILTER", "Hello, World!")
                .hasNoMoreMessages();
    }

    @Test public void logsUnknownHostExceptions() {
        Logger.plant(new TestTree());
        Logger.e(new UnknownHostException(), null);

        assertExceptionLogged(Logger.Priority.ERROR, "", "UnknownHostException");
    }

    @Test public void tagIsClearedWhenNotLoggable() {
        Logger.plant(new TestTree() {
            @Override
            protected boolean isLoggable(int priority) {
                return priority >= Logger.Priority.WARN;
            }
        });
        Logger.tag("NotLogged").i("Message not logged");
        Logger.w("Message logged");

        assertLog()
                .hasWarnMessage("LoggerTest", "Message logged")
                .hasNoMoreMessages();
    }

    @Test public void logsWithCustomFormatter() {
        Logger.plant(new TestTree() {
            @Override
            protected String formatMessage(String message, Object[] args) {
                return String.format("Test formatting: " + message, args);
            }
        });
        Logger.d("Test message logged. %d", 100);

        assertLog()
                .hasDebugMessage("LoggerTest", "Test formatting: Test message logged. 100");
    }

    private static String repeat(char c, int number) {
        char[] data = new char[number];
        Arrays.fill(data, c);
        return new String(data);
    }

    private void assertExceptionLogged(int logType, String message, String exceptionClassname) {
        assertExceptionLogged(logType, message, exceptionClassname, null, 0);
    }

    private void assertExceptionLogged(int logType, String message, String exceptionClassname, String tag, int index) {
        List<LogItem> logs = logAssert.getLogs();
        assertThat(logs).hasSize(index + 1);
        LogItem log = logs.get(index);
        assertThat(log.type).isEqualTo(Logger.Priority.name(logType));
        assertThat(log.tag).isEqualTo(tag != null ? tag : "LoggerTest");

        if (message != null) {
            assertThat(log.msg).startsWith(message);
        }

        assertThat(log.msg).contains(exceptionClassname);
        // We use a low-level primitive that Robolectric doesn't populate.
        assertThat(log.throwable).isNull();
    }

    private LogAssert assertLog() {
        return logAssert;
    }

    public static class LogItem {
        public final String type;
        public final String tag;
        public String msg;
        public Throwable throwable;

        public LogItem(String type, String tag, String msg, Throwable throwable) {
            this.type = type;
            this.tag = tag;
            this.msg = msg;
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            return "LogItem{" +
                    "type='" + type + '\'' +
                    ", tag='" + tag + '\'' +
                    ", msg='" + msg + '\'' +
                    ", throwable=" + throwable +
                    '}';
        }
    }

    private static final class LogAssert {

        String re1="(\\[)";
        String re2="((?:[a-z][a-z0-9_]*))";
        String re3="(\\|)";
        String re4="((?:[a-z][a-z0-9_]*))";
        String re5="(.*)?";
        String re6="(\\])";
        String re7="( )";
        String re8="(.*)";

        private int index = 0;

        private LogAssert() { }

        public List<LogItem> getLogs() {
            return TestTree.items;
        }

        public LogAssert hasVerboseMessage(String tag, String message) {
            return hasMessage(Logger.Priority.VERBOSE, tag, message);
        }

        public LogAssert hasDebugMessage(String tag, String message) {
            return hasMessage(Logger.Priority.DEBUG, tag, message);
        }

        public LogAssert hasInfoMessage(String tag, String message) {
            return hasMessage(Logger.Priority.INFO, tag, message);
        }

        public LogAssert hasWarnMessage(String tag, String message) {
            return hasMessage(Logger.Priority.WARN, tag, message);
        }

        public LogAssert hasErrorMessage(String tag, String message) {
            return hasMessage(Logger.Priority.ERROR, tag, message);
        }

        public LogAssert hasAssertMessage(String tag, String message) {
            return hasMessage(Logger.Priority.ASSERT, tag, message);
        }

        private LogAssert hasMessage(int priority, String tag, String message) {
            LogItem item = getLogs().get(index++);
            assertThat(item.type).isEqualTo(Logger.Priority.name(priority));
            assertThat(item.tag).isEqualTo(tag);
            assertThat(item.msg).isEqualTo(message);
            return this;
        }

        public void hasNoMoreMessages() {
            assertThat(getLogs()).hasSize(index);
        }
    }
    private LogAssert logAssert = new LogAssert();
}