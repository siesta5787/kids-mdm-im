# Kids MDM IM: FOSS stand-ins for Google libraries

Kids MDM IM ships **no Google code**. `kidsmdm/settings.gradle.kts` substitutes every Google
artifact Signal depends on with one of these modules. Each exposes only the API Signal calls,
and behaves as if Google Play services are missing: availability checks return
`SERVICE_MISSING`, requests fail, and map views stay empty. Signal then takes its normal
"no Google Play" paths: websocket instead of FCM, typed-in registration codes instead of SMS
auto-read, no Google Pay, and all languages built in instead of Play language downloads.

| Module | Replaces |
|---|---|
| `gms-tasks` | `play-services-tasks`: a working Tasks implementation, needed by kotlinx-coroutines-play-services |
| `gms-base` | `play-services-base`/`-basement`: `GoogleApiAvailability` (always missing), status/exception types, `ProviderInstaller` |
| `gms-auth` | `play-services-auth`/`-auth-api-phone`: SMS retriever and phone-number hint (always fail) |
| `gms-maps` | `play-services-maps`: empty `MapView`/`SupportMapFragment` (location sharing is disabled) |
| `gms-wallet` | `play-services-wallet`: Google Pay (never ready) |
| `play-feature-delivery` | `com.google.android.play:feature-delivery`/`core-common`: split install (always unavailable) |
| `firebase-messaging` | `firebase-messaging`: no FCM token, ever |
| `billing` | Signal's `:lib:billing` project (Google Play Billing): Signal's own `Empty` implementations |
| `empty` | `credentials-play-services-auth`, `googleid` (not needed on Android 14+) |

**When a Signal merge stops compiling** because Signal started using another Google API, add just
that class or method to the matching stub with the same signature, behaving as "unavailable".
Never add the real Google dependency back. `kids-test.yml` checks that no Google artifact reaches
the release APK.

Licensing: `gms-tasks` (`com.google.android.gms.tasks.*`, `org.microg.gms.tasks.*`,
`common.internal.Preconditions`) is microG's Apache-2.0 implementation, taken from Molly's
`core-gms/tasks`. Everything else here is new and AGPL-3.0-only like the rest of the app.
