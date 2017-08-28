package org.apache.maven.plugin.surefire.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Maintains per-thread test result state. Not thread safe.
 */
public class TestSetStats
{
    private static final String TESTS = "Tests ";
    private static final String RUN = "run: ";
    private static final String TESTS_RUN = "Tests run: ";
    private static final String FAILURES = "Failures: ";
    private static final String ERRORS = "Errors: ";
    private static final String SKIPPED = "Skipped: ";
    private static final String FAILURE_MARKER = " <<< FAILURE!";
    private static final String IN_MARKER = " - in ";
    private static final String COMMA = ", ";

    private final Queue<WrappedReportEntry> reportEntries = new ConcurrentLinkedQueue<WrappedReportEntry>();

    private final boolean trimStackTrace;

    private final boolean plainFormat;

    private long testSetStartAt;

    private long testStartAt;

    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

    private long lastStartAt;

    public TestSetStats( final boolean trimStackTrace, final boolean plainFormat )
    {
        this.trimStackTrace = trimStackTrace;
        this.plainFormat = plainFormat;
    }

    public int getElapsedSinceTestSetStart()
    {
        return testSetStartAt > 0 ? (int) ( System.currentTimeMillis() - testSetStartAt ) : 0;
    }

    public int getElapsedSinceLastStart()
    {
        return lastStartAt > 0 ? (int) ( System.currentTimeMillis() - lastStartAt ) : 0;
    }

    public void testSetStart()
    {
        testSetStartAt = System.currentTimeMillis();
        lastStartAt = testSetStartAt;
    }

    public void testStart()
    {
        testStartAt = System.currentTimeMillis();
        lastStartAt = testStartAt;
    }

    private long finishTest( final WrappedReportEntry reportEntry )
    {
        reportEntries.add( reportEntry );
        incrementCompletedCount();
        final long testEndAt = System.currentTimeMillis();
        // SUREFIRE-398 skipped tests call endTest without calling testStarting
        // if startTime = 0, set it to endTime, so the diff will be 0
        if ( testStartAt == 0 )
        {
            testStartAt = testEndAt;
        }
        final Integer elapsedTime = reportEntry.getElapsed();
        return elapsedTime != null ? elapsedTime : testEndAt - testStartAt;
    }

    public void testSucceeded( final WrappedReportEntry reportEntry )
    {
        finishTest( reportEntry );
    }

    public void testError( final WrappedReportEntry reportEntry )
    {
        errors += 1;
        finishTest( reportEntry );
    }

    public void testFailure( final WrappedReportEntry reportEntry )
    {
        failures += 1;
        finishTest( reportEntry );
    }

    public void testSkipped( final WrappedReportEntry reportEntry )
    {
        skipped += 1;
        finishTest( reportEntry );
    }

    public void reset()
    {
        completedCount = 0;
        errors = 0;
        failures = 0;
        skipped = 0;

        for ( final WrappedReportEntry entry : reportEntries )
        {
            entry.getStdout().free();
            entry.getStdErr().free();
        }

        reportEntries.clear();
    }

    public int getCompletedCount()
    {
        return completedCount;
    }

    public int getErrors()
    {
        return errors;
    }

    public int getFailures()
    {
        return failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public String elapsedTimeAsString( final long runTime )
    {
        return ReporterUtils.formatElapsedTime( runTime );
    }

    private void incrementCompletedCount()
    {
        completedCount += 1;
    }

    public String getTestSetSummary( final WrappedReportEntry reportEntry )
    {
        String summary = TESTS_RUN + completedCount
                                 + COMMA
                                 + FAILURES + failures
                                 + COMMA
                                 + ERRORS + errors
                                 + COMMA
                                 + SKIPPED + skipped
                                 + COMMA
                                 + reportEntry.getElapsedTimeVerbose();

        if ( failures > 0 || errors > 0 )
        {
            summary += FAILURE_MARKER;
        }

        summary += IN_MARKER + reportEntry.getNameWithGroup();

        return summary;
    }

//    public String getColoredTestSetSummary( final WrappedReportEntry reportEntry )
//    {
//        final boolean isSuccessful = failures == 0 && errors == 0 && skipped == 0;
//        final boolean isFailure = failures > 0;
//        final boolean isError = errors > 0;
//        final boolean isFailureOrError = isFailure | isError;
//        final boolean isSkipped = skipped > 0;
//        final  MessageBuilder builder = buffer();
//        if ( isSuccessful )
//        {
//            if ( completedCount == 0 )
//            {
//                builder.strong( TESTS_RUN ).strong( completedCount );
//            }
//            else
//            {
//                builder.success( TESTS_RUN ).success( completedCount );
//            }
//        }
//        else
//        {
//            if ( isFailureOrError )
//            {
//                builder.failure( TESTS ).strong( RUN ).strong( completedCount );
//            }
//            else
//            {
//                builder.warning( TESTS ).strong( RUN ).strong( completedCount );
//            }
//        }
//        builder.a( COMMA );
//        if ( isFailure )
//        {
//            builder.failure( FAILURES ).failure( failures );
//        }
//        else
//        {
//            builder.a( FAILURES ).a( failures );
//        }
//        builder.a( COMMA );
//        if ( isError )
//        {
//            builder.failure( ERRORS ).failure( errors );
//        }
//        else
//        {
//            builder.a( ERRORS ).a( errors );
//        }
//        builder.a( COMMA );
//        if ( isSkipped )
//        {
//            builder.warning( SKIPPED ).warning( skipped );
//        }
//        else
//        {
//            builder.a( SKIPPED ).a( skipped );
//        }
//        builder.a( COMMA )
//                .a( reportEntry.getElapsedTimeVerbose() );
//        if ( isFailureOrError )
//        {
//            builder.failure( FAILURE_MARKER );
//        }
//        builder.a( IN_MARKER );
//        return concatenateWithTestGroup( builder, reportEntry );
//    }

    public List<String> getTestResults()
    {
        final List<String> result = new ArrayList<String>();
        for ( final WrappedReportEntry testResult : reportEntries )
        {
            if ( testResult.isErrorOrFailure() )
            {
                result.add( testResult.getOutput( trimStackTrace ) );
            }
            else if ( plainFormat && testResult.isSkipped() )
            {
                result.add( testResult.getName() + " skipped" );
            }
            else if ( plainFormat && testResult.isSucceeded() )
            {
                result.add( testResult.getElapsedTimeSummary() );
            }
        }
        return result;
    }

    public Collection<WrappedReportEntry> getReportEntries()
    {
        return reportEntries;
    }

    /**
     * Append the test set message for a report.
     * e.g. "org.foo.BarTest ( of group )"
     *
     * @param builder    MessageBuilder with preceded text inside
     * @param report     report whose test set is starting
     * @return the message
     */
//    public static String concatenateWithTestGroup( final MessageBuilder builder, final ReportEntry report )
//    {
//        final String testClass = report.getNameWithGroup();
//        final int delimiter = testClass.lastIndexOf( '.' );
//        final String pkg = testClass.substring( 0, 1 + delimiter );
//        final String cls = testClass.substring( 1 + delimiter );
//        return builder.a( pkg )
//                       .strong( cls )
//                       .toString();
//    }
}
