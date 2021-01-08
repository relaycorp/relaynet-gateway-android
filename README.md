<img src="./relaynet-logo.png" align="right"/>

# Relaynet Gateway for Android

The [Relaynet Gateway for Android](https://play.google.com/store/apps/details?id=tech.relaycorp.gateway) is a _[private gateway](https://specs.relaynet.network/RS-000#concepts)_ for Android 5+ devices. This repository contains the source code for the app, which is also a reference implementation of a private gateway in the Relaynet protocol suite.

This document is aimed at advanced users and (prospective) contributors. We aim to make the app as simple and intuitive as possible, and we're therefore not planning on publishing end-user documentation at this point. To learn more about _using_ Relaynet, visit [relaynet.network/users](https://relaynet.network/users).

## Relaynet bindings

This private gateway implements [Relaynet bindings](https://specs.relaynet.network/RS-000#message-transport-bindings) as follows:

- Local endpoints communicate with the private gateway via a [PoWeb](https://specs.relaynet.network/RS-016) server on `127.0.0.1:13276`. This server is implemented with the PoWeb binding and [ktor](https://ktor.io).
- When the Internet is available and the public gateway is reachable, this private gateway will communicate with its public counterpart using [Relaycorp's PoWeb client](https://docs.relaycorp.tech/relaynet-poweb-jvm/).
- When communicating with couriers over WiFi, this private gateway uses the [CogRPC binding](https://specs.relaynet.network/RS-008) through [Relaycorp's CogRPC client](https://docs.relaycorp.tech/relaynet-cogrpc-jvm/).

The local communication with endpoints does not use TLS, but all other connections are external and therefore require TLS.

By default, instances of this gateway are paired to [Relaycorp's Frankfurt gateway](https://github.com/relaycorp/cloud-gateway/tree/main/environments/frankfurt).

## Security and privacy considerations

The items below summarize the security and privacy considerations specific to this app. For a more general overview of the security considerations in Relaynet, please refer to [RS-019](https://specs.relaynet.network/RS-019).

### No encryption at rest on Android 5

We use the [Android Keystore system](https://developer.android.com/training/articles/keystore) to protect sensitive cryptographic material, such as long-term and ephemeral keys. Unfortunately, [Android 5 doesn't actually encrypt anything at rest](https://github.com/relaycorp/relaynet-gateway-android/issues/247).

### External communication

In addition to communicating with its public gateway, this app communicates with the following:

- `https://dns.google/dns-query` as the DNS-over-HTTPS (DoH) resolver, which [we plan to replace with Cloudflare's](https://github.com/relaycorp/relaynet-gateway-android/issues/249). DoH is only used to resolve SRV records for the public gateway (e.g., [`_rgsc._tcp.frankfurt.relaycorp.cloud`](https://mxtoolbox.com/SuperTool.aspx?action=srv%3a_rgsc._tcp.frankfurt.relaycorp.cloud&run=toolpage)), as we delegate the DNSSEC validation to the DoH resolver.
- `https://google.com`. When the public gateway can't be reached, this app will make periodic GET requests to Google to check if the device is connected to the Internet and thus provide the user with a more helpful message about the reason why things aren't working. We chose `google.com` because of its likelihood to be available and uncensored.
- The host running the DHCP server on port `21473`, when the device is connected to a WiFi network but disconnected from the Internet. We do this to check whether the device is connected to the WiFi hotspot of a courier.
- Other apps on the same device can potentially communicate with the local PoWeb server provided by this app on `127.0.0.1:13276`. Because this server uses the HTTP and WebSocket protocols, we block web browser requests by disabling CORS and refusing WebSocket connections with the `Origin` header (per the PoWeb specification).

This app doesn't track usage (for example, using Google Analytics), nor does it use ads.

### App signing

We use [app signing by Google Play](https://support.google.com/googleplay/android-developer/answer/9842756) to distribute this app on Google Play. We use [gradle-play-publisher](https://github.com/Triple-T/gradle-play-publisher) as part of our [automated release process](.github/workflows/ci-cd.yml) to upload an Android App Bundle to the Play Store using an upload key stored on this GitHub project secret.

This app [may be available on F-Droid in the future](https://github.com/relaycorp/relayverse/issues/21).

## Limitations

### Courier synchronization over non-DHCP WiFi connections is unsupported

Unfortunately, [Android doesn't offer a reliable way to get the default internet gateway in the local network](https://stackoverflow.com/questions/61615270/how-to-get-the-ip-address-of-the-default-gateway-reliably-on-android-5), so we have to rely on DHCP and assume the DHCP server has the same IP address as the courier. This is reliable enough for the average user, but it means that advanced users won't be able to skip DHCP when connecting to their couriers over WiFi.

## Development

The project requires [Android Studio](https://developer.android.com/studio/) 4+.

## Contributing

We love contributions! If you haven't contributed to a Relaycorp project before, please take a minute to [read our guidelines](https://github.com/relaycorp/.github/blob/master/CONTRIBUTING.md) first.
