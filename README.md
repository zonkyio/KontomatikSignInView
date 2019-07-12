# Kontomatik SignIn View

Simple way how to integrate Kontomatik SignIn Widget into an android application. Allows you to easy handle all javascript callbacks and add required params to reach desired behavior of the widget from kotlin API.

See https://developer.kontomatik.com/ for futher details about Kontomatik Banking API or https://developer.kontomatik.com/api-doc/#signin-widget for SignIn Widget

##  Download

**Step 1.** Add the JitPack repository to your build file: Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

**Step 2.** Add the dependency

	dependencies {
	        implementation 'com.github.radimjanda754:KontomatikSignInView:0.2'
	}

##  Usage (Kotlin examples)

Declare KontomatikSignInView in your XML layout, then use it in your fragment/activity

    var signInView: KontomatikSignInView

Add desired params via Kontomatik documentation https://developer.kontomatik.com/api-doc/#embedding-the-widget (except client_id)

    signInView.addParam("country", "cz")

For params that take javascript code itself (for example setting styles in css) you can use

    signInView.addJavascriptParam("styles", 
        """
        {
            bodyBgColor: '#FFFFFF',
            btnBgColor: '#F8F8F8'
        }
        """
    )

Simply handle callbacks with lambda functions. All lambdas will be executed on UI Thread. Lambda for `setOnError` also provides `handledInView` boolean to determine if the error was caused by user and will be handled by widget itself.

    signInView.setOnSuccess { target, sessionId, sessionIdSignature, optionsJson ->
                        // TODO
    }

    signInView.setOnError { exception, optionsJson, handledInView ->
        if (!handledInView) {
            // TODO
        }
    }

Then load view with your client id and some html file where javascript code will be generated.

    signInView.loadKontomatik(KONTOMATIK_CLIENT_ID, File(context.applicationContext.filesDir, "kontomatik.html"))