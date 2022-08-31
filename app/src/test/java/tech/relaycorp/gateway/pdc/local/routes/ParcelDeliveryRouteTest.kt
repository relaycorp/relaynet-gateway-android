package tech.relaycorp.gateway.pdc.local.routes

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.domain.StoreParcel
import tech.relaycorp.gateway.pdc.local.utils.ContentType
import tech.relaycorp.relaynet.messages.Parcel
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.PDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import kotlin.test.assertEquals
import io.ktor.http.ContentType as KtorContentType

class ParcelDeliveryRouteTest {
    private val endpointPath = "/v1/parcels"
    private val plainTextUTF8ContentType = KtorContentType.Text.Plain.withCharset(Charsets.UTF_8)

    private val storeParcel = mock<StoreParcel>()
    private val route = ParcelDeliveryRoute(storeParcel)

    private val parcel =
        Parcel(Recipient("0deadbeef", "example.com"), byteArrayOf(), PDACertPath.PRIVATE_ENDPOINT)
    private val parcelSerialized = parcel.serialize(KeyPairSet.PRIVATE_ENDPOINT.private)

    @Test
    fun `Invalid request Content-Type should be refused with an HTTP 415 response`() {
        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, endpointPath) {
                addHeader("Content-Type", KtorContentType.Application.Json.toString())
            }
            with(call) {
                assertEquals(HttpStatusCode.UnsupportedMediaType, response.status())
                assertEquals(plainTextUTF8ContentType, response.contentType())
                assertEquals(
                    "Content type ${ContentType.PARCEL} is required",
                    response.content
                )
            }
        }
    }

    @Test
    fun `Malformed parcels should be refused with an HTTP 400 response`() = runBlockingTest {
        whenever(storeParcel.store(any<ByteArray>(), eq(RecipientLocation.ExternalGateway)))
            .thenReturn(StoreParcel.Result.MalformedParcel(Exception()))

        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, endpointPath) {
                addHeader("Content-Type", ContentType.PARCEL.toString())
            }
            with(call) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(KtorContentType.Text.Plain, response.contentType().withoutParameters())
                assertEquals("Parcel is malformed", response.content)
            }
        }
    }

    @Test
    fun `Invalid parcels should be refused with an HTTP 422 response`() = runBlockingTest {
        whenever(storeParcel.store(any<ByteArray>(), eq(RecipientLocation.ExternalGateway)))
            .thenReturn(StoreParcel.Result.InvalidParcel(parcel, Exception()))

        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, endpointPath) {
                addHeader("Content-Type", ContentType.PARCEL.toString())
            }
            with(call) {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertEquals(KtorContentType.Text.Plain, response.contentType().withoutParameters())
                assertEquals("Parcel is invalid", response.content)
            }
        }
    }

    @Test
    fun `Valid parcels should result in an HTTP 202 response`() = runBlockingTest {
        whenever(storeParcel.store(parcelSerialized, RecipientLocation.ExternalGateway))
            .thenReturn(StoreParcel.Result.Success(parcel))

        testPDCServerRoute(route) {
            val call = handleRequest(HttpMethod.Post, endpointPath) {
                addHeader("Content-Type", ContentType.PARCEL.toString())
            }
            with(call) {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }
        }
    }
}
