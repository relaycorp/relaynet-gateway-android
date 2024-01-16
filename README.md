<img src="./awala-logo.png" align="right"/>

# Awala Gateway for Android

The [Awala Gateway for Android](https://play.google.com/store/apps/details?id=tech.relaycorp.gateway) is a _[private gateway](https://specs.awala.network/RS-000#concepts)_ for Android 6+ devices. This repository contains the source code for the app, which is also a reference implementation of a private gateway in the Awala protocol suite.

This document is aimed at advanced users and (prospective) contributors. We aim to make the app as simple and intuitive as possible, and we're therefore not planning on publishing end-user documentation at this point. To learn more about _using_ Awala, visit [awala.network/users](https://awala.network/users).

## Awala bindings

This private gateway implements [Awala bindings](https://specs.awala.network/RS-000#message-transport-bindings) as follows:

- Local endpoints communicate with the private gateway via a [PoWeb](https://specs.awala.network/RS-016) server on `127.0.0.1:13276`. This server is implemented with the PoWeb binding and [ktor](https://ktor.io).
- When the Internet is available and the public gateway is reachable, this private gateway will communicate with its public counterpart using [Relaycorp's PoWeb client](https://docs.relaycorp.tech/relaynet-poweb-jvm/).
- When communicating with couriers over WiFi, this private gateway uses the [CogRPC binding](https://specs.awala.network/RS-008) through [Relaycorp's CogRPC client](https://docs.relaycorp.tech/relaynet-cogrpc-jvm/).

The local communication with endpoints does not use TLS, but all other connections are external and therefore require TLS.

By default, instances of this gateway are paired to [Relaycorp's Frankfurt gateway](https://github.com/relaycorp/cloud-gateway/tree/main/environments/frankfurt).

## Security and privacy considerations

The items below summarize the security and privacy considerations specific to this app. For a more general overview of the security considerations in Awala, please refer to [RS-019](https://specs.awala.network/RS-019).

### External communication

In addition to communicating with its public gateway, this app communicates with the following:

- `https://dns.google/dns-query` as the DNS-over-HTTPS (DoH) resolver, which [we plan to replace with Cloudflare's](https://github.com/relaycorp/relaynet-gateway-android/issues/249). DoH is only used to resolve SRV records for the public gateway (e.g., [`_awala-gsc._tcp.belgium.relaycorp.services`](https://mxtoolbox.com/SuperTool.aspx?action=srv%3a_awala-gsc._tcp.belgium.relaycorp.services&run=toolpage)), as we delegate the DNSSEC validation to the DoH resolver.
- `https://google.com`. When the public gateway can't be reached, this app will make periodic GET requests to Google to check if the device is connected to the Internet and thus provide the user with a more helpful message about the reason why things aren't working. We chose `google.com` because of its likelihood to be available and uncensored.
- The host running the DHCP server on port `21473`, when the device is connected to a WiFi network but disconnected from the Internet. We do this to check whether the device is connected to the WiFi hotspot of a courier.
- Other apps on the same device can potentially communicate with the local PoWeb server provided by this app on `127.0.0.1:13276`. Because this server uses the HTTP and WebSocket protocols, we block web browser requests by disabling CORS and refusing WebSocket connections with the `Origin` header (per the PoWeb specification).

This app doesn't track usage (for example, using Google Analytics), nor does it use ads.

### App signing

We use [app signing by Google Play](https://support.google.com/googleplay/android-developer/answer/9842756) to distribute this app on Google Play. We use [gradle-play-publisher](https://github.com/Triple-T/gradle-play-publisher) as part of our [automated release process](.github/workflows/ci-cd.yml) to upload an Android App Bundle to the Play Store using an upload key stored as a GitHub project secret.

This app [may be available on F-Droid in the future](https://github.com/relaycorp/relayverse/issues/21).

## Limitations

### Courier synchronization over non-DHCP WiFi connections is unsupported

Unfortunately, [Android doesn't offer a reliable way to get the default internet gateway in the local network](https://stackoverflow.com/questions/61615270/how-to-get-the-ip-address-of-the-default-gateway-reliably-on-android-5), so we have to rely on DHCP and assume the DHCP server has the same IP address as the courier. This is reliable enough for the average user, but it means that advanced users won't be able to skip DHCP when connecting to their couriers over WiFi.

## Naming rationale

We're referring to this app as "Awala" in the user interface, even though this is obviously one of the components that make up the network, in order to hide technical details from the end user. The terms "private gateway" or "gateway" may be more accurate, but we don't think they sound user-friendly.

However, we do use the terms "private gateway" or "gateway" in the code base because we absolutely need accuracy there.

## Architecture

The app follows clean architecture principles. Domain logic is separated from external elements
such as UI and data. The main components / layers / packages are:

 - `domain` - Domain logic, with one class per use-case
 - `ui` - Presentation logic, organized per screen, and following an [MVVM](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) pattern
 - `data` - Data persistence logic using preferences, database and disk
 - `background` - Background services and state listeners
 - `pdc` - Implementation of the Parcel Delivery Connection between endpoints and the gateway

Components are tied by dependency injection using [Dagger](https://dagger.dev).
Kotlin coroutines and flow are used for threading and reactive design.
For the views, material components were preferred whenever possible.

## Development

The project requires [Android Studio](https://developer.android.com/studio/) 4+.

## Contributing

We love contributions! If you haven't contributed to a Relaycorp project before, please take a minute to [read our guidelines](https://github.com/relaycorp/.github/blob/master/CONTRIBUTING.md) first.
