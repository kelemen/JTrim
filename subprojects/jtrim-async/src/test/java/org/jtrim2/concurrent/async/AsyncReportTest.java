package org.jtrim2.concurrent.async;

import org.junit.Test;

import static org.junit.Assert.*;

public class AsyncReportTest {
    @Test
    public void testSUCCESSConstant() {
        AsyncReport report = AsyncReport.SUCCESS;
        assertNull(report.getException());
        assertFalse(report.isCanceled());
        assertTrue(report.isSuccess());
        assertNotNull(report.toString());
    }

    @Test
    public void testCANCELEDConstant() {
        AsyncReport report = AsyncReport.CANCELED;
        assertNull(report.getException());
        assertTrue(report.isCanceled());
        assertFalse(report.isSuccess());
        assertNotNull(report.toString());
    }

    @Test
    public void testCreatedSuccessful() {
        AsyncReport report = AsyncReport.getReport(null, false);
        assertNull(report.getException());
        assertFalse(report.isCanceled());
        assertTrue(report.isSuccess());
        assertNotNull(report.toString());
    }

    @Test
    public void testCreatedCanceled() {
        AsyncReport report = AsyncReport.getReport(null, true);
        assertNull(report.getException());
        assertTrue(report.isCanceled());
        assertFalse(report.isSuccess());
        assertNotNull(report.toString());
    }

    @Test
    public void testCreatedCanceledWithException() {
        Exception exception = new Exception();
        AsyncReport report = AsyncReport.getReport(exception, true);
        assertSame(exception, report.getException());
        assertTrue(report.isCanceled());
        assertFalse(report.isSuccess());
        assertNotNull(report.toString());
    }

    @Test
    public void testCreatedNotCanceledWithException() {
        Exception exception = new Exception();
        AsyncReport report = AsyncReport.getReport(exception, false);
        assertSame(exception, report.getException());
        assertFalse(report.isCanceled());
        assertFalse(report.isSuccess());
        assertNotNull(report.toString());
    }
}
