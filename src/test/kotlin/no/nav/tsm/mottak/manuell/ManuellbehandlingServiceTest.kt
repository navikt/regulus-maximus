package no.nav.tsm.mottak.manuell

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class ManuellbehandlingServiceTest {

    @Test
    fun testGetCSVFile() {
        val storageMock = Mockito.mock(Storage::class.java)
        val blob = Mockito.mock(Blob::class.java)
        Mockito.`when`(blob.getContent())
            .thenReturn("1,2022-01-01 12:00:00 +00:00\n2,2022-01-01 12:00:00 +00:00\n".toByteArray())
        Mockito.`when`(storageMock.get("regulus-maximus-bucket-dev", "manuellbehandling.csv"))
            .thenReturn(blob)
        val manuellbehandlingService = ManuellbehandlingService(storageMock, "regulus-maximus-bucket-dev", "manuellbehandling.csv")
        val manuellbehandlinger = manuellbehandlingService.getManuellBehandlingTimestamp("1")
        assertEquals("2022-01-01T12:00Z", manuellbehandlinger.toString())
    }
}
