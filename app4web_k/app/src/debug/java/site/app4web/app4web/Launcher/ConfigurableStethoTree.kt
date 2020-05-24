package site.app4web.app4web.Launcher

import android.util.Log
import com.facebook.stetho.inspector.console.CLog
import com.facebook.stetho.inspector.console.ConsolePeerManager
import com.facebook.stetho.inspector.protocol.module.Console
import timber.log.Timber
import timber.log.Timber.DebugTree

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
/**
 * NOTE: Using this ONLY UNTIL this PR: https://github.com/facebook/stetho/pull/490
 * gets merged into Stetho.
 * ПРИМЕЧАНИЕ: использование этого ТОЛЬКО до этого PR: https://github.com/facebook/stetho/pull/490
 * объединяется в Stetho.
 */
/**
 * Timber tree implementation which forwards logs to the Chrome Dev console.
 * Реализация дерева тимберса, которая пересылает логи на консоль Chrome Dev.
 * This uses a [Timber.DebugTree] to automatically infer the tag from the calling class.
 * При этом используется [Timber.DebugTree] для автоматического вывода тега из вызывающего класса.
 * Plant it using [Timber.plant]
 * Посадите его, используя [# plant (Timber.Tree)][Timber]
 * Timber tree implementation which forwards logs to the Chrome Dev console.
 * Реализация дерева тимберса, которая пересылает логи на консоль Chrome Dev.
 * * This uses a [Timber.DebugTree] to automatically infer the tag from the calling class.
 * * При этом используется [Timber.DebugTree] для автоматического вывода тега из вызывающего класса.
 * * Plant it using [Timber.plant]
 * * Посадите его, используя [# plant (Timber.Tree)][Timber]
 * <pre>
 * `Timber.plant(new StethoTree())
 * //or
 * Timber.plant(new StethoTree(
 * new StethoTree.Configuration.Builder()
 * .showTags(true)
 * .minimumPriority(Log.WARN)
 * .build()));
` *
</pre> *
 */
// Вызывается только из DebugLauncher 1 раз создается новый объект
class ConfigurableStethoTree : DebugTree {
    private val mConfiguration // см класс ниже здесь же
            : Configuration?

    constructor() {
        mConfiguration =
            Configuration.Builder().build()
    }

    constructor(configuration: Configuration?) {
        mConfiguration = configuration
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (priority < mConfiguration!!.mMinimumPriority) {
            return
        }
        val peerManager = ConsolePeerManager.getInstanceOrNull()
        if (peerManager == null) {
            Log.println(priority, tag, message)
            return
        }
        val logLevel: Console.MessageLevel
        logLevel = when (priority) {
            Log.VERBOSE, Log.DEBUG -> Console.MessageLevel.DEBUG
            Log.INFO -> Console.MessageLevel.LOG
            Log.WARN -> Console.MessageLevel.WARNING
            Log.ERROR, Log.ASSERT -> Console.MessageLevel.ERROR
            else -> Console.MessageLevel.LOG
        }
        val messageBuilder = StringBuilder()
        if (mConfiguration.mShowTags && tag != null) {
            messageBuilder
                .append("[")
                .append(tag)
                .append("] ")
        }
        messageBuilder.append(message)
        CLog.writeToConsole(
            logLevel,
            Console.MessageSource.OTHER,
            messageBuilder.toString()
        )
    }

    class Configuration private constructor(val mShowTags: Boolean, val mMinimumPriority: Int) {

        class Builder {
            private var mShowTags = false
            private var mMinimumPriority = Log.VERBOSE

            /**
             * @param showTags Logs the tag of the calling class when true.
             * Default is false.
             * @return This [Builder] instance.
             * @param showTags Регистрирует тег вызывающего класса, когда он равен true.
             * По умолчанию установлено значение false.
             * @return Это [Builder] экземпляр.
             */
            fun showTags(showTags: Boolean): Builder {
                mShowTags = showTags
                return this
            }

            /**
             * @param priority Minimum log priority to send log.
             * Expects one of constants defined in [Log].
             * Default is [Log.VERBOSE].
             * @return This [Builder] instance.
             * @param priority Минимальный приоритет журнала для отправки журнала.
             *                 Ожидается одна из констант, определенных в [Log].
             *                 По умолчанию [# VERBOSE][Log].
             * @return Это [Builder] экземпляр.
             */
            fun minimumPriority(priority: Int): Builder {
                mMinimumPriority = priority
                return this
            }

            fun build(): Configuration {
                return Configuration(
                    mShowTags,
                    mMinimumPriority
                )
            }
        }

    }
}