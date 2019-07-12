package cz.zonky.kontomatiksigninview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.io.File

/**
 * Simple way how to integrate Kontomatik SingIn widget into an Android application
 * @see <a href="https://developer.kontomatik.com/api-doc/#signin-widget">Kontomatik SignIn widget</a>
 *
 * @author Radim Janda [radim.janda@zonky.cz]
 * @since 28/2/2019
 */
open class KontomatikSignInView(context: Context) : WebView(context) {

    companion object {
        /**
         *  Use to handle javascript callbacks
         */
        const val JS_RECEIVER: String = "JSReceiver"
        /**
         * Used in [kontomatikJsTemplate] as client id placeholder
         */
        const val TEMPLATE_CLIENT_ID = "[CLIENT]"
        /**
         * Used in [kontomatikJsTemplate] as placeholder where all other added parameters will be generated
         */
        const val TEMPLATE_OTHER_PARAMS = "[PARAMS]"
        /**
         * Error exceptions caused by user.
         * These exceptions are handled in widget and doesn't need to be handled in callback
         */
        val USER_CAUSED_EXCEPTIONS = arrayOf("AccessBlocked", "AccessTemporarilyBlocked", "ManualActionRequired",
                "InvalidCredentials", "TargetCredentialsMismatch", "UnsupportedLoginMethod", "UnsupportedLanguage", "InsufficientIdentificationLevel")
    }

    private var onSuccessEvent: (target: String, sessionId: String, sessionIdSignature: String, optionsJson: String) -> Unit = { _, _, _, _ -> }
    private var onErrorEvent: (exception: String, optionsJson: String, handledInView: Boolean) -> Unit = { _, _, _ -> }
    private var onUnsupportedTargetEvent: (target: String, country: String, address: String) -> Unit = { _, _, _ -> }
    private var onInitializedEvent: () -> Unit = {}
    private var onStartedEvent: () -> Unit = {}
    private var onTargetSelectedEvent: (name: String, officialName: String) -> Unit = { _, _ -> }
    private var onCredentialEnteredEvent: () -> Unit = {}

    private var kontomatikParams: HashMap<String, String> = HashMap()

    /**
     * Kontomatik javascript code will be generated from this template
     */
    var kontomatikJsTemplate: String = """
    <html>
        <head><script src="https://signin.kontomatik.com/assets/signin-widget.js"></script></head>
        <body><div id="kontomatik" />
            <script type="text/javascript">embedKontomatik({
                client: '$TEMPLATE_CLIENT_ID',
                divId: 'kontomatik',
                $TEMPLATE_OTHER_PARAMS
                onSuccess: function(target, sessionId, sessionIdSignature, options) {
                    $JS_RECEIVER.onSuccess(target, sessionId, sessionIdSignature, JSON.stringify(options));
                },
                onError: function(exception, options) {
                    $JS_RECEIVER.onError(exception, JSON.stringify(options));
                },
                onUnsupportedTarget: function(target, country, address) {
                    $JS_RECEIVER.onUnsupportedTarget(target, country, address)
                },
                onInitialized: function() {
                    $JS_RECEIVER.onInitialized()
                },
                onStarted: function() {
                    $JS_RECEIVER.onStarted()
                },
                onTargetSelected: function(name, officialName) {
                    $JS_RECEIVER.onTargetSelected(name, officialName)
                },
                onCredentialEntered: function() {
                    $JS_RECEIVER.onCredentialEntered()
                }
            });
            </script>
        </body>
    </html>
    """

    /**
     * @param clientId Your Kontomatik client id
     * @param targetFile Html file where Kontomatik javascript code will be generated and then loaded
     */
    open fun loadKontomatik(clientId: String, targetFile: File) {
        val finalParams = kontomatikParams.map { entry ->
            "${entry.key}: ${entry.value},\n"
        }.joinToString("")
        val finalJsData = kontomatikJsTemplate.replace(TEMPLATE_CLIENT_ID, clientId).replace(TEMPLATE_OTHER_PARAMS, finalParams)
        targetFile.writeText(finalJsData)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        addJavascriptInterface(KontomatikSignInCallbacks(), JS_RECEIVER)
        loadUrl(targetFile.toURI().toString())
    }

    /**
     * Use to add primitive params.
     * @see <a href="https://developer.kontomatik.com/api-doc/#embedding-the-widget">Kontomatik API Doc</a>
     * @param param Optional parameter name
     * @param value Value of parameter
     */
    fun addParam(param: String, value: String): KontomatikSignInView {
        kontomatikParams[param] = "'$value'"
        return this
    }

    /**
     * Use to add primitive params.
     * @see <a href="https://developer.kontomatik.com/api-doc/#embedding-the-widget">Kontomatik API Doc</a>
     * @param param Optional parameter name
     * @param value Value of parameter
     */
    fun addParam(param: String, value: Boolean): KontomatikSignInView {
        kontomatikParams[param] = value.toString()
        return this
    }

    /**
     * Use to add primitive params.
     * @see <a href="https://developer.kontomatik.com/api-doc/#embedding-the-widget">Kontomatik API Doc</a>
     * @param param Optional parameter name
     * @param value Value of parameter
     */
    fun addParam(param: String, value: Int): KontomatikSignInView {
        kontomatikParams[param] = value.toString()
        return this
    }

    /**
     * Use to add optional parameters with javascript code as value. Requires correct javascript format.
     * @see <a href="https://developer.kontomatik.com/api-doc/#embedding-the-widget">Kontomatik API Doc</a>
     * @param param Optional parameter name
     * @param javascriptCode Value of parameter, requires correct javascript code
     */
    fun addJavascriptParam(param: String, javascriptCode: String): KontomatikSignInView {
        kontomatikParams[param] = javascriptCode
        return this
    }

    /**
     * Clears all previously set params for Kontomatik API
     */
    fun clearParams() {
        kontomatikParams.clear()
    }

    /**
     * Sets event that shoud happen after login was successful
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnSuccess(onSuccessEvent: (target: String, sessionId: String, sessionIdSignature: String, optionsJson: String) -> Unit) {
        this.onSuccessEvent = onSuccessEvent
    }

    /**
     * Sets event that shoud happen when some error occurs
     * @param [onErrorEvent.handledInView] determines whether caused exception is one of [USER_CAUSED_EXCEPTIONS] and was already handled by view itself
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnError(onErrorEvent: (exception: String, optionsJson: String, handledInView: Boolean) -> Unit) {
        this.onErrorEvent = onErrorEvent
    }

    /**
     * Sets event that shoud happen when user selects “My bank is not listed…” option
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnUnsupportedTarget(setOnUnsupportedTarget: (target: String, country: String, address: String) -> Unit) {
        this.onUnsupportedTargetEvent = setOnUnsupportedTarget
    }

    /**
     * Sets event that should happen when Kontomatik bank selection is shown
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnStarted(onStartedEvent: () -> Unit) {
        this.onStartedEvent = onStartedEvent
    }

    /**
     * Sets event that should happen when user selects bank from the list.
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnTargetSelected(onTargetSelectedEvent: (name: String, officialName: String) -> Unit) {
        this.onTargetSelectedEvent = onTargetSelectedEvent
    }

    /**
     * Sets event that should happen after user enters credential
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnCredentialEntered(onCredentialEnteredEvent: () -> Unit) {
        this.onCredentialEnteredEvent = onCredentialEnteredEvent
    }

    /**
     * Sets event that should happen when Kontomatik is initialized and ready for use
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    fun setOnInitialized(onInitializedEvent: () -> Unit) {
        this.onInitializedEvent = onInitializedEvent
    }

    /**
     * Handles callbacks from javascript code
     * Extend to handle other callbacks
     * @see <a href="https://developer.kontomatik.com/api-doc/#callbacks">Kontomatik SignIn Callbacks</a>
     */
    private inner class KontomatikSignInCallbacks {

        @JavascriptInterface
        fun onSuccess(target: String, sessionId: String, sessionIdSignature: String, optionsJson: String) {
            context.runOnUi {
                onSuccessEvent(target, sessionId, sessionIdSignature, optionsJson)
            }
        }

        @JavascriptInterface
        fun onError(exception: String, optionsJson: String) {
            context.runOnUi {
                onErrorEvent(exception, optionsJson, USER_CAUSED_EXCEPTIONS.contains(exception))
            }
        }

        @JavascriptInterface
        fun onUnsupportedTarget(target: String, country: String, address: String) {
            context.runOnUi {
                onUnsupportedTargetEvent(target, country, address)
            }
        }

        @JavascriptInterface
        fun onInitialized() {
            context.runOnUi {
                onInitializedEvent()
            }
        }

        @JavascriptInterface
        fun onStarted() {
            context.runOnUi {
                onStartedEvent()
            }
        }

        @JavascriptInterface
        fun onTargetSelected(name: String, officialName: String) {
            context.runOnUi {
                onTargetSelectedEvent(name, officialName)
            }
        }

        @JavascriptInterface
        fun onCredentialEntered() {
            context.runOnUi {
                onCredentialEnteredEvent()
            }
        }
    }

    private fun Context.runOnUi(f: Context.() -> Unit) {
        if (Looper.getMainLooper() === Looper.myLooper()) f() else Handler(Looper.getMainLooper()).post { f() }
    }
}