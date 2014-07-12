NOTES
=====

STUFF TO DO
===========

general
---

* setup testing framework
* finish configuration module
* finish shell module
* create assembly
v use [Paranamer](https://github.com/paul-hammant/paranamer) instead of Spring's ParameterNameDiscoverer
v fix project assembly
* provisioning module

lifecycle
---

* Add @Cachable facility
* Weave constructors too, not only methods
v Weaver can cache ALL bytecode cache files in memory, for one single I/O read
    * if a class not found in memory cache, still look for it on disk of course

validation
---

* Support validation of constructors

web
---

* return 406 when no acceptable content encodings are supported
* create a REST entity framework
* implement @TypedInterceptor and @UriInterceptor
* implement statistics for request handling
    * maybe use Jetty's org.eclipse.jetty.util.statistic.[CounterStatistic|SampleStatistic]
* implement security
    * add digest protocol support
    * add client certificate support
* implement request.unmarshallBody( type )
* implement form authentication
* duplication of @Secured - both in ControllerAdapter.getSecurityConstraint and SecurityMethodInterceptor
