package com.atistudios.mondly.languages.chatbot.keyboard

import android.annotation.TargetApi
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.PopupWindow
import com.atistudios.mondly.languages.chatbot.R


/**
 * The keyboard height provider, this class uses a PopupWindow
 * to calculate the window height when the floating keyboard is opened and closed.
 */
// forked from here https://github.com/siebeprojects/samples-keyboardheight
class KeyboardHeightProvider
/**
 * Construct a new KeyboardHeightProvider
 *
 * @param activity The parent activity
 */
    (
    /** The root activity that uses this KeyboardHeightProvider  */
    private val activity: Activity
) : PopupWindow(activity) {

    /** The keyboard height observer  */
    private var observer: KeyboardHeightObserver? = null

    /** The cached landscape height of the keyboard  */
    private var keyboardLandscapeHeight: Int = 0

    /** The cached portrait height of the keyboard  */
    private var keyboardPortraitHeight: Int = 0

    /** The view that is used to calculate the keyboard height  */
    private val popupView: View?

    /** The parent view  */
    private val parentView: View

    /**
     * Get the screen orientation
     *
     * @return the screen orientation
     */
    private val screenOrientation: Int
        get() = activity.resources.configuration.orientation

    init {

        val inflator = activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        this.popupView = inflator.inflate(R.layout.popupwindow, null, false)
        contentView = popupView

        softInputMode = LayoutParams.SOFT_INPUT_ADJUST_RESIZE or LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED

        parentView = activity.findViewById(android.R.id.content)

        width = 0
        height = LayoutParams.MATCH_PARENT

        popupView!!.viewTreeObserver.addOnGlobalLayoutListener {
            if (popupView != null) {
                handleOnGlobalLayout()
            }
        }
    }

    /**
     * Start the KeyboardHeightProvider, this must be called after the onResume of the Activity.
     * PopupWindows are not allowed to be registered before the onResume has finished
     * of the Activity.
     */
    fun start() {

        if (!isShowing && parentView.windowToken != null) {
            setBackgroundDrawable(ColorDrawable(0))
            showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
        }
    }

    /**
     * Close the keyboard height provider,
     * this provider will not be used anymore.
     */
    fun close() {
        this.observer = null
        dismiss()
    }

    /**
     * Set the keyboard height observer to this provider. The
     * observer will be notified when the keyboard height has changed.
     * For example when the keyboard is opened or closed.
     *
     * @param observer The observer to be added to this provider.
     */
    fun setKeyboardHeightObserver(observer: KeyboardHeightObserver?) {
        this.observer = observer
    }

    @TargetApi(android.os.Build.VERSION_CODES.P)
    private fun getTopCutoutHeight(): Int {
        val decorView = activity.window.decorView ?: return 0
        var cutOffHeight = 0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val windowInsets = decorView.rootWindowInsets
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val displayCutout = windowInsets.displayCutout
                if (displayCutout != null) {
                    val list = displayCutout.boundingRects
                    for (rect in list) {
                        if (rect.top == 0) {
                            cutOffHeight += rect.bottom - rect.top
                        }
                    }
                }
            }

        }
        return cutOffHeight
    }

    /**
     * Popup window itself is as big as the window of the Activity.
     * The keyboard can then be calculated by extracting the popup view bottom
     * from the activity window height.
     */
    private fun handleOnGlobalLayout() {

        val screenSize = Point()
        activity.windowManager.defaultDisplay.getSize(screenSize)

        val rect = Rect()
        popupView!!.getWindowVisibleDisplayFrame(rect)

        // REMIND, you may like to change this using the fullscreen size of the phone
        // and also using the status bar and navigation bar heights of the phone to calculate
        // the keyboard height. But this worked fine on a Nexus.
        val orientation = screenOrientation
        var topCutoutHeight = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            topCutoutHeight = getTopCutoutHeight()
        }
        val keyboardHeight = screenSize.y + topCutoutHeight - rect.bottom

        if (keyboardHeight == 0) {
            notifyKeyboardHeightChanged(0, orientation)
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.keyboardPortraitHeight = keyboardHeight
            notifyKeyboardHeightChanged(keyboardPortraitHeight, orientation)
        } else {
            this.keyboardLandscapeHeight = keyboardHeight
            notifyKeyboardHeightChanged(keyboardLandscapeHeight, orientation)
        }
    }

    /**
     *
     */
    private fun notifyKeyboardHeightChanged(height: Int, orientation: Int) {
        if (observer != null) {
            observer!!.onKeyboardHeightChanged(height, orientation)
        }
    }

    companion object {

        /** The tag for logging purposes  */
        private val TAG = "sample_KeyboardHeightProvider"
    }
}