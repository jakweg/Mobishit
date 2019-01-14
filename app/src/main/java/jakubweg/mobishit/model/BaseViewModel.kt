package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import android.support.annotation.WorkerThread

@Suppress("NOTHING_TO_INLINE")
abstract class BaseViewModel(application: Application)
    : AndroidViewModel(application) {

    protected val context: Application get() = getApplication()

    private var currentTask: BackgroundTask? = null

    protected inline fun <T> handleBackground(returnValue: T): T {
        handleBackground()
        return returnValue
    }

    protected fun handleBackground() {
        if (currentTask == null)
            currentTask = BackgroundTask(this).apply { execute() }
    }

    protected fun cancelLastTask() {
        currentTask?.cancel(false)
        currentTask = null
    }

    protected fun rerunTask() {
        currentTask?.cancel(false)
        currentTask = BackgroundTask(this).apply { execute() }
    }

    @WorkerThread
    protected abstract fun doInBackground()

    private class BackgroundTask(
            private val instance: BaseViewModel)
        : AsyncTask<Unit, Unit, Unit>(){
        override fun doInBackground(vararg params: Unit?) {
            instance.doInBackground()
        }
    }
}

inline val <reified T> MutableLiveData<T>.asImmutable: LiveData<T> get() = this