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
