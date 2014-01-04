NOTES
=====

This project currently relies on a patch I sent to [Maven Bundle Plugin](http://felix.apache.org/site/apache-felix-maven-bundle-plugin-bnd.html)
which allows turning off the setting of "optional" resolution in the OSGi manifest for packages that were imported from
<optional> maven dependencies.

This patch can be applied locally until they add it for now - see [FELIX-4372](https://issues.apache.org/jira/browse/FELIX-4372)

STUFF TO DO
===========

general
---

* use [Paranamer](https://github.com/paul-hammant/paranamer) instead of Spring's ParameterNameDiscoverer

lifecycle
---

* Add @Cachable facility
* Weave constructors too, not only methods

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
