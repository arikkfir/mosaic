ROAD-MAP
========

* [IN-PROGRESS] Web handlers
    * [IN-PROGRESS] Handlers
        * Support for `@Service` handlers
            * Auto-translate return values to `ServiceResponse` objects
            * translates `AccessDeniedException` into a ServiceResponse with "access denied" message
            * return values implementing `MessageContributor` add messages to the service response
            * translates ServiceException(s) into a `ServiceResponse` with exception message
            * translates any other Exception into a `ServiceResponse` with "internal error" message
        * Versioned handlers (either through header and or URL prefix)
        * Return 406 (Method not allowed) in case some handlers match the URL but not the HTTP method
    * Security:
        * Authentication should be implemented with @UserRepository methods
            * @UserRepository has @Filter to only support certain apps (e.g. "app.params['global.users'] == true")
        * Handlers can throw AccessDeniedException
        * Think about using Apache Shiro
    * [IN-PROGRESS] Marshalling
        * Create FreemarkerMarshaller that marshalls Template(type=freemarker)
        * Create VelocityMarshaller that marshalls Template(type=velocity)
        * Create ThymaleafMarshaller that marshalls Template(type=thymaleaf)
        * Create a bundle resource marshaller
    * [IN-PROGRESS] Content Manager System (CMS)
        * [IN-PROGRESS] Create CMS model parser
        * Implement the CMS sites manager
        * Create a CMS handler (finds pages and CMS resources)
        * Create a CMS page marshaller
    * Create an XML SCHEMA file (XSD) for HTTP application files

DONE
====

* [DONE] Bootstrapping
    * [DONE] Launcher
    * [DONE] Bundle dependencies framework
        * [DONE] Bundles with `META-INF/spring/*.xml` are supported
        * [DONE] Support for `@ContextRef` for injecting bundle contexts
        * [DONE] Support for `@ServiceRef` for injecting services
        * [DONE] Support for `@ServiceBind` and `@ServiceUnbind` for tracking services
        * [DONE] Support for `@ServiceExport` (with `@Rank`) for publishing services
* [DONE] Configuration framework
    * [DONE] Live configuration objects
    * [DONE] Consumed through the standard `@ServiceRef` dependency mechanism
* [DONE] Transaction support
    * [DONE] Automatic code weaving for enabling database transaction support
    * [DONE] Detected through the `@Transactional` annotation
* [DONE] Shell framework
    * [DONE] SSH listener
    * [DONE] Extensible command detection (`@ShellCommand`)
* [IN-PROGRESS] Web handlers
    * [DONE] Jetty listener
    * [IN-PROGRESS] Handlers
        * [DONE] Support for handler filters using `@Filter`
        * [DONE] Support for `@Controller`
        * [DONE] Support for native `Handler` implementations
    * [DONE] Exception handlers
        * [DONE] Support for `@ExceptionHandler`s
        * [DONE] Support for native implementations of `ExceptionHandler`s
    * [DONE] URL interceptors
        * [DONE] Support for `@Interceptor`s
        * [DONE] Support for native implementations of `Interceptor`s
    * [IN-PROGRESS] Marshalling
        * [DONE] Support for `@Marshaller`s
        * [DONE] Support for native implementations of `Marshaller`s
        * [DONE] Declaration of supported marshallable types
        * [DONE] Declaration of supported content types (via "Accept" header)
    * [IN-PROGRESS] Content Manager System (CMS)
        * [DONE] Create the CMS API
        * [DONE] Create base CMS model implementation
