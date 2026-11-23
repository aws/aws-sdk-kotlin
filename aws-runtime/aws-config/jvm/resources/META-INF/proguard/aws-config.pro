# Java 16+ Unix domain socket API, referenced by the credential daemon transport. The SDK only calls it after
# checking that the running JVM has it, and Android doesn't, so R8/ProGuard must not fail on the missing class.
-dontwarn java.net.UnixDomainSocketAddress
