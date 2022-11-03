package jenkins.widgets;

import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.search.UserSearchProperty;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AuthorizationStrategy;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * TODO: Code partially duplicated with HistoryPageFilterTest in core
 *
 * Search in case insensitive mode is tested by unit tests in HistoryPageFilterTest.
 */
@Issue({"JENKINS-40718", "JENKINS-42645"})
public class HistoryPageFilterCaseSensitiveSearchTest {

    private static final String TEST_USER_NAME = "testUser";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_search_case_sensitively_when_enabled_for_user() throws IOException {
        setCaseSensitiveSearchForUserAndCheckAssertionForGivenSearchString("FAILURE", new SearchResultAssertFunction() {
            @Override
            public void doAssertion(HistoryPageFilter<ModelObject> historyPageFilter) {
                Assert.assertEquals(1, historyPageFilter.runs.size());
                Assert.assertEquals(HistoryPageEntry.getEntryId(2), historyPageFilter.runs.get(0).getEntryId());
            }
        });
    }

    @Test
    public void should_skip_result_with_different_capitalization_when_case_sensitively_search_is_enabled_for_user() throws IOException {
        setCaseSensitiveSearchForUserAndCheckAssertionForGivenSearchString("failure", new SearchResultAssertFunction() {
            @Override
            public void doAssertion(HistoryPageFilter<ModelObject> historyPageFilter) {
                Assert.assertEquals(0, historyPageFilter.runs.size());
            }
        });
    }

    private void setCaseSensitiveSearchForUserAndCheckAssertionForGivenSearchString(final String searchString,
                                                                                    SearchResultAssertFunction assertionOnSearchResults) throws IOException {
        AuthorizationStrategy.Unsecured strategy = new AuthorizationStrategy.Unsecured();
        j.jenkins.setAuthorizationStrategy(strategy);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        UsernamePasswordAuthenticationToken testUserAuthentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, "any");
        try (ACLContext ignored = ACL.as2(testUserAuthentication)) {
            User.getOrCreateByIdOrFullName(TEST_USER_NAME).addProperty(new UserSearchProperty(false));

            //test logic
            final Iterable<ModelObject> runs = Arrays.asList(new MockRun(2, Result.FAILURE), new MockRun(1, Result.SUCCESS));
            assertNoMatchingBuildsForGivenSearchStringAndRunItems(searchString, runs, assertionOnSearchResults);
        }

    }

    private void assertNoMatchingBuildsForGivenSearchStringAndRunItems(String searchString, Iterable<ModelObject> runs,
                                                                       SearchResultAssertFunction assertionOnSearchResults) {
        //given
        HistoryPageFilter<ModelObject> historyPageFilter = new HistoryPageFilter<>(5);
        //and
        historyPageFilter.setSearchString(searchString);

        //when
        historyPageFilter.add(runs, Collections.emptyList());

        //then
        assertionOnSearchResults.doAssertion(historyPageFilter);
    }

    @SuppressWarnings("unchecked")
    private static class MockRun extends Run {
        private final long queueId;

        MockRun(long queueId) throws IOException {
            super(Mockito.mock(Job.class));
            this.queueId = queueId;
        }

        MockRun(long queueId, Result result) throws IOException {
            this(queueId);
            this.result = result;
        }

        @Override
        public int compareTo(Run o) {
            return 0;
        }

        @Override
        public Result getResult() {
            return result;
        }

        @Override
        public boolean isBuilding() {
            return false;
        }

        @Override
        public long getQueueId() {
            return queueId;
        }

        @Override
        public int getNumber() {
            return (int) queueId;
        }
    }

    //Waiting for Java 8... - coming soon - April 2017?
    private interface SearchResultAssertFunction {
        void doAssertion(HistoryPageFilter<ModelObject> historyPageFilter);
    }
}
