# Module coroutines

Swaps `Dispatchers.Main` for a test dispatcher from shared `commonTest` code on JVM, Android, and iOS.
`runTestWithMain` installs the dispatcher around one test, and `MainDispatcherFixture` pairs `install()` and
`reset()` with `@BeforeTest` and `@AfterTest`. Add it to shared `commonTest` and any coroutine tests.
