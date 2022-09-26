package site.app4web.app4web.Launcher;
/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * Этот исходный код лицензирован в соответствии с лицензией в стиле BSD, найденной в
 * ЛИЦЕНЗИОННЫЙ файл в корневом каталоге этого исходного дерева. Дополнительный грант
 * патентных прав можно найти в файле PATENTS в том же каталоге.
 */
import android.util.Log;
import com.facebook.stetho.inspector.console.CLog;
import com.facebook.stetho.inspector.console.ConsolePeerManager;
import com.facebook.stetho.inspector.protocol.module.Console;
import timber.log.Timber;

/**
 * NOTE: Using this ONLY UNTIL this PR: https://github.com/facebook/stetho/pull/490
 * gets merged into Stetho.
 * ПРИМЕЧАНИЕ: использование этого ТОЛЬКО до этого PR: https://github.com/facebook/stetho/pull/490
 * объединяется в Stetho.
 */

/**
 * Timber tree implementation which forwards logs to the Chrome Dev console.
 * Реализация дерева тимберса, которая пересылает логи на консоль Chrome Dev.
 * This uses a {@link Timber.DebugTree} to automatically infer the tag from the calling class.
 * При этом используется {@link Timber.DebugTree} для автоматического вывода тега из вызывающего класса.
 * Plant it using {@link Timber#plant(Timber.Tree)}
 * Посадите его, используя {@link Timber # plant (Timber.Tree)}
 * Timber tree implementation which forwards logs to the Chrome Dev console.
 * Реализация дерева тимберса, которая пересылает логи на консоль Chrome Dev.
 *  * This uses a {@link Timber.DebugTree} to automatically infer the tag from the calling class.
 *  * При этом используется {@link Timber.DebugTree} для автоматического вывода тега из вызывающего класса.
 *  * Plant it using {@link Timber#plant(Timber.Tree)}
 *  * Посадите его, используя {@link Timber # plant (Timber.Tree)}
 * <pre>
 *   {@code
 *   Timber.plant(new StethoTree())
 *   //or
 *   Timber.plant(new StethoTree(
 *       new StethoTree.Configuration.Builder()
 *           .showTags(true)
 *           .minimumPriority(Log.WARN)
 *           .build()));
 *   }
 * </pre>
 */
// Вызывается только из DebugLauncher 1 раз создается новый объект
public class ConfigurableStethoTree extends Timber.DebugTree {
    private final Configuration mConfiguration; // см класс ниже здесь же

    public ConfigurableStethoTree() {
        this.mConfiguration = new Configuration.Builder().build();
    }

    public ConfigurableStethoTree(Configuration configuration) {
        this.mConfiguration = configuration;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {

        if(priority < mConfiguration.mMinimumPriority) {
            return;
        }

        ConsolePeerManager peerManager = ConsolePeerManager.getInstanceOrNull();
        if (peerManager == null) {
            Log.println(priority, tag, message);
            return;
        }

        Console.MessageLevel logLevel;

        switch (priority) {
            case Log.VERBOSE:
            case Log.DEBUG:
                logLevel = Console.MessageLevel.DEBUG;
                break;
            case Log.INFO:
                logLevel = Console.MessageLevel.LOG;
                break;
            case Log.WARN:
                logLevel = Console.MessageLevel.WARNING;
                break;
            case Log.ERROR:
            case Log.ASSERT:
                logLevel = Console.MessageLevel.ERROR;
                break;
            default:
                logLevel = Console.MessageLevel.LOG;
        }

        StringBuilder messageBuilder = new StringBuilder();

        if(mConfiguration.mShowTags && tag != null) {
            messageBuilder
                .append("[")
                .append(tag)
                .append("] ");
        }

        messageBuilder.append(message);

        CLog.writeToConsole(
            logLevel,
            Console.MessageSource.OTHER,
            messageBuilder.toString()
        );

    }

    public static class Configuration {

        private final boolean mShowTags;
        private final int mMinimumPriority;

        private Configuration(boolean showTags, int minimumPriority) {
            this.mShowTags = showTags;
            this.mMinimumPriority = minimumPriority;
        }

        public static class Builder {

            private boolean mShowTags = false;
            private int mMinimumPriority = Log.VERBOSE;

            /**
             * @param showTags Logs the tag of the calling class when true.
             *                 Default is false.
             * @return This {@link Builder} instance.
             * @param showTags Регистрирует тег вызывающего класса, когда он равен true.
             *                 По умолчанию установлено значение false.
             * @return Это {@link Builder} экземпляр.
             */
            public Builder showTags(boolean showTags) {
                this.mShowTags = showTags;
                return this;
            }

            /**
             * @param priority Minimum log priority to send log.
             *                 Expects one of constants defined in {@link Log}.
             *                 Default is {@link Log#VERBOSE}.
             * @return This {@link Builder} instance.
             * @param priority Минимальный приоритет журнала для отправки журнала.
             *                 Ожидается одна из констант, определенных в {@link Log}.
             *                 По умолчанию {@link Log # VERBOSE}.
             * @return Это {@link Builder} экземпляр.
             */
            public Builder minimumPriority(int priority) {
                this.mMinimumPriority = priority;
                return this;
            }

            public Configuration build() {
                return new Configuration(mShowTags, mMinimumPriority);
            }
        }
    }
}
