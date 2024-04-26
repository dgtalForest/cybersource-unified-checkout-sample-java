# Unified Checkout Sample Application

Unified Checkout is a Cybersource-hosted JavaScript library that embeds an all-in-one payment widget on your checkout page.
It also handles all of the sensitive payment information and calls Cybersource's Flex API on your behalf to make accepting a variety of 
payment methods safer and easier.

This product has support for manual card entry, Click to Pay, and digital wallets. Support for additional payment types is in the development pipeline.

This example integration uses the `cybersource-rest-client` dependency to invoke Unified Checkout.

The application uses Spring Boot, Thymeleaf, and Lombok to minimize boilerplate and highlight how your controller and service layers might be structured using a Java backend.

For more details on Unified Checkout see our developer guide [here](https://developer.cybersource.com/docs/cybs/en-us/unified-checkout/developer/all/rest/unified-checkout/uc-intro.html).

For users of the Click to Pay Drop-in UI, the developer guide is available [here](https://developer.cybersource.com/docs/cybs/en-us/click-to-pay/developer/all/rest/click-to-pay.html).

## Prerequisites
- [Java 16 or higher](https://www.oracle.com/java/technologies/javase/jdk16-archive-downloads.html)
  ( the application uses records and text blocks to increase readability, but these could be refactored away if this application needs to be ran in Java 8, etc.)
- [Maven 3.x](https://maven.apache.org/install.html)


## Setup Instructions
1. Uncomment the properties below in [application.properties](./src/main/resources/application.properties) with your CyberSource REST credentials created in the
   [EBC Portal](https://ebc2test.cybersource.com/) (learn more about how to get an account [here](https://developer.cybersource.com/hello-world.html)).
    ```properties
    app.merchantID=YOUR MERCHANT ID
    app.merchantKeyId=YOUR KEY ID (SHARED SECRET SERIAL NUMBER)
    app.merchantSecretKey=YOUR SECRET KEY
    ```
   If you do not have a merchant ID, set one up or [reach out](https://developer.cybersource.com/support/contact-us.html) 
   for information about our `testrest` merchant to get up and running quickly.<p/>
2. (Optional) You may need to alter the [default capture context request](./src/main/resources/default-uc-capture-context-request.json) file to fit your merchant's configuration, but this can also be done in the browser. 
   For example, by default, Google Pay is not enabled out of the box for new merchants, so you may need to remove it from `allowedPaymentTypes`
   or configure it in EBC. You can learn more about the Capture Context [here](https://developer.cybersource.com/docs/cybs/en-us/unified-checkout/developer/all/rest/unified-checkout/uc-getting-started-integration-flow/uc-setup-capture-context.html).
3. Build and run the application using Maven. This will automatically deploy a local Tomcat server with port 8080 exposed.
    ```shell
    mvn spring-boot:run
    ```
   <p></p>
## Using the Application
> ❗️ This application uses Cybersource's test environment, and should be used with mock data.
> 
> All information is secured to our production standards, but 
> please use a [test card number](https://developer.cybersource.com/hello-world/testing-guide.html) such as `4111 1111 1111 1111`
> and false name / address information on the `/checkout` page (where information is entered into Unified Checkout's UI) to limit usage of any unnecessary personal information.
>
> To use Google Pay test card data, see [Google's guide](https://developers.google.com/pay/api/android/guides/resources/test-card-suite) 
> on how to add the test card suite group to your Google Pay profile. 
> 
Unified Checkout requires an HTTPS URL, so navigate to https://localhost:8080 and proceed through the various screens
to understand how things work under the hood. Note that you may receive a warning about the certificate's validity, and can simply proceed.
Otherwise you can add the `ucDemoKeystore.p12` in `./src/main/resources/keystore` using Keychain Access (Mac) or MMC (Windows).

To serve from a different domain, or change other request attributes, see [default capture context request](./src/main/resources/default-uc-capture-context-request.json).
The `targetOrigins` field in this request controls where your checkout page is served.

### Test Cards
- `4111 1111 1111 1111` - Visa test card, frictionless 3DS
- `4456 5300 0000 1096` - Visa test card, 3DS challenge will be issued
- `5200 0000 0000 1096` - Mastercard test card, 3DS challenge will be issued
- [Other 3DS test scenarios](https://developer.cybersource.com/library/documentation/dev_guides/Payer_Authentication_SCMP_API/html/Topics/Test_Cases_for_3D_Secure_2_x.htm)

### Additional Steps for Click to Pay Drop-in UI users
1. Follow the steps outlined in the [Click to Pay Drop-in UI developer guide](https://developer.cybersource.com/docs/cybs/en-us/click-to-pay/developer/all/rest/click-to-pay.html) to
create a portfolio-level merchant with the proper configurations, upload a public encryption key, and create a transacting merchant as a child of this portfolio
2. Uncomment the following properties in [application.properties](./src/main/resources/application.properties) _in addition to_ the merchant properties specified in [Setup Instructions](#setup-instructions).
    ```properties
   app.porfolioId=YOUR PORTFOLIO ID
   app.portfolioKeyId=YOUR PORTFOLIO KEY ID
   app.portfolioSecretKey=YOUR PORTFOLIO SECRET KEY
    ```
3. Upload your portfolio merchant's private key to a file called `private_key.pem` in the [keystore folder](./src/main/resources). The key should match the specs outlined in [the developer guide](https://developer.cybersource.com/docs/cybs/en-us/click-to-pay/developer/all/rest/click-to-pay/ctp-configuration-intro/uc-upload-encrypt-key-intro.html) 