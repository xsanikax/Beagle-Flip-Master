package com.beagleflipper.model;

import com.beagleflipper.controller.ApiRequestHandler;
import com.beagleflipper.controller.DoesNothingExecutorService;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlipPriceGraphControllerTest {

    private static final String DISPLAY_NAME_1 = "Acc 1";
    private static final String DISPLAY_NAME_2 = "Acc 2";
    private static final String DISPLAY_NAME_3 = "Acc 3";
    private final Map<String, Integer> displayNameToAccountId = Map.of(DISPLAY_NAME_1, 0, DISPLAY_NAME_2, 1, DISPLAY_NAME_3, 2);

    @Test
    public void testOneAccount() {

        // generate 6 months or random flips
        int now = (int) Instant.now().getEpochSecond();
        int sixMonthsAgo = (int) Instant.now().minus(365/ 2, ChronoUnit.DAYS).getEpochSecond();
        List<FlipV2> flips = generateFlipsBetween(sixMonthsAgo, now, 10_000, List.of(0));

        // create and populate the flip cache
        // FIX: Provide all required dependencies for FlipManager constructor
        ApiRequestHandler mockApiRequestHandler = Mockito.mock(ApiRequestHandler.class);
        ScheduledExecutorService mockExecutorService = new DoesNothingExecutorService();
        OkHttpClient mockOkHttpClient = new OkHttpClient.Builder().build();
        OsrsLoginManager mockOsrsLoginManager = Mockito.mock(OsrsLoginManager.class); // Mock the new dependency

        FlipManager flipManager = new FlipManager(
                mockApiRequestHandler,
                mockExecutorService,
                mockOkHttpClient,
                mockOsrsLoginManager // Pass the new mock dependency
        );
        flipManager.setFlipsChangedCallback(() -> {});
        flipManager.displayNameToAccountId.putAll(displayNameToAccountId);
        flipManager.mergeFlips(flips, null);
        flipManager.setIntervalDisplayName(DISPLAY_NAME_1);


        verifyflipManagerStoredOrder(flipManager);

        flips.sort(Comparator.comparing(FlipV2::getClosedTime).reversed().thenComparing(FlipV2::getId));

        // create list of test interval start times
        List<Integer> testTimes = Stream.generate(()-> randomIntBetween(sixMonthsAgo, now)).limit(100).collect(Collectors.toList());
        testTimes.add(0, 0); // add 0 which is ALL

        for (int time : testTimes) {
            flipManager.setIntervalStartTime(time);

            // check stats equal
            Stats stats1 = flipManager.getIntervalStats();
            Stats stats2 = expectedStats(flips, time, DISPLAY_NAME_1);
            Assert.assertEquals(stats1, stats2);

            // check all flips equal
            List<FlipV2> allFlips1 = flipManager.getPageFlips(1, flips.size());
            List<FlipV2> allFlips2 = expectedPage(flips, time, 1, flips.size(), DISPLAY_NAME_1);
            assertFlipListsEqual(allFlips1, allFlips2);

            // check paginated flips equal
            for (int pageSize : Arrays.asList(3, 20, 33, 50, 100)) {
                int numPages = stats1.flipsMade / pageSize + 1;
                for(int page=1; page <= numPages; page++) {
                    List<FlipV2> pageFlips2 = expectedPage(flips, time, page, pageSize, DISPLAY_NAME_1);
                    List<FlipV2> pageFlips1 = flipManager.getPageFlips(page, pageSize);
                    assertFlipListsEqual(pageFlips1, pageFlips2);
                }
            }
        }
    }

    @Test
    public void testMultipleAccounts() {

        // generate 6 months or random flips
        int now = (int) Instant.now().getEpochSecond();
        int sixMonthsAgo = (int) Instant.now().minus(365/ 2, ChronoUnit.DAYS).getEpochSecond();
        List<FlipV2> flips = generateFlipsBetween(sixMonthsAgo, now, 5_000, List.of(0, 1, 2));

        // create and populate the flip cache
        // FIX: Provide all required dependencies for FlipManager constructor
        ApiRequestHandler mockApiRequestHandler = Mockito.mock(ApiRequestHandler.class);
        ScheduledExecutorService mockExecutorService = new DoesNothingExecutorService();
        OkHttpClient mockOkHttpClient = new OkHttpClient.Builder().build();
        OsrsLoginManager mockOsrsLoginManager = Mockito.mock(OsrsLoginManager.class); // Mock the new dependency

        FlipManager flipManager = new FlipManager(
                mockApiRequestHandler,
                mockExecutorService,
                mockOkHttpClient,
                mockOsrsLoginManager // Pass the new mock dependency
        );
        flipManager.setFlipsChangedCallback(() -> {});
        flipManager.displayNameToAccountId.putAll(displayNameToAccountId);
        flipManager.mergeFlips(flips, null);
        verifyflipManagerStoredOrder(flipManager);


        flips.sort(Comparator.comparing(FlipV2::getClosedTime).reversed().thenComparing(Comparator.comparing(FlipV2::getId).reversed()));

        // create list of test interval start times
        List<Integer> testTimes = Stream.generate(()-> randomIntBetween(sixMonthsAgo, now)).limit(100).collect(Collectors.toList());
        testTimes.add(0, 0); // add 0 which is ALL

        flipManager.setIntervalDisplayName(null);
        List<FlipV2> flips1 = flipManager.getPageFlips(1, flips.size());
        assertFlipListsEqual(flips1, flips);

        for (String name : Arrays.asList(DISPLAY_NAME_1, DISPLAY_NAME_2, DISPLAY_NAME_3, null)) {
            flipManager.setIntervalDisplayName(name);
            for (int time : testTimes) {
                flipManager.setIntervalStartTime(time);

                // check stats equal
                Stats stats1 = flipManager.getIntervalStats();
                Stats stats2 = expectedStats(flips, time, name);
                Assert.assertEquals(stats1, stats2);

                // check all flips equal
                List<FlipV2> allFlips1 = flipManager.getPageFlips(1, flips.size());
                List<FlipV2> allFlips2 = expectedPage(flips, time, 1, flips.size(), name);
                assertFlipListsEqual(allFlips1, allFlips2);

                // check paginated flips equal
                for (int pageSize : Arrays.asList(3, 20, 33, 50, 100)) {
                    int numPages = stats1.flipsMade / pageSize + 1;
                    for (int page = 1; page <= numPages; page++) {
                        List<FlipV2> pageFlips1 = flipManager.getPageFlips(page, pageSize);
                        List<FlipV2> pageFlips2 = expectedPage(flips, time, page, pageSize, name);
                        assertFlipListsEqual(pageFlips1, pageFlips2);
                    }
                }
            }
        }
    }

    public void verifyflipManagerStoredOrder(FlipManager flipManager) {
        for (int i =0; i < flipManager.weeks.size(); i++) {
            Assert.assertTrue(flipManager.weeks.get(Math.max(i-1,0)).weekStart <= flipManager.weeks.get(i).weekStart);
            FlipManager.WeekAggregate w = flipManager.weeks.get(i);
            for (List<FlipV2> flips : w.accountIdToFlips.values()) {
                for (int ii =1; ii < flips.size(); ii++) {
                    Assert.assertTrue(flips.get(ii-1).getClosedTime() <= flips.get(ii).getClosedTime());
                }
            }
        }
    }

    private List<FlipV2> expectedPage(List<FlipV2> flips, int time, int pageNumber, int pageSize, String displayName) {
        Integer accountId = displayName == null ? null : displayNameToAccountId.getOrDefault(displayName, -1);
        int toSkip = (pageNumber - 1) * pageSize;
        List<FlipV2> page = new ArrayList<>();
        for(FlipV2 f : flips) {
            if(f.getClosedTime() > time && (accountId == null || accountId == f.getAccountId())) {
                if(toSkip > 0) {
                    toSkip -= 1;
                } else {
                    page.add(f);
                    if(page.size() == pageSize) {
                        break;
                    }
                }
            }
        }
        return page;
    }

    private void assertFlipListsEqual(List<FlipV2> f1, List<FlipV2> f2) {
        if(f1.size() != f2.size()) {
            Assert.fail("flips lists not equal length");
        }
        for (int i=0; i < f1.size(); i++) {
            FlipV2 flip1 = f1.get(i);
            FlipV2 flip2 = f2.get(i);
            if (!flip1.equals(flip2)) {
                Assert.fail("flips don't match at index " + i);
            }
        }
    }

    private Stats expectedStats(List<FlipV2> flips, int time, String displayName) {
        Integer accountId = displayName == null ? null : displayNameToAccountId.getOrDefault(displayName, -1);
        Stats stats = new Stats(0,0,0,0);
        for(FlipV2 f : flips) {
            if(f.getClosedTime() > time && (accountId == null || accountId == f.getAccountId())) {
                stats.flipsMade += 1;
                stats.gross += f.getSpent();
                stats.profit += f.getProfit();
            }
        }
        return stats;
    }

    private List<FlipV2> generateFlipsBetween(int start, int end, int number, List<Integer> accountIds) {
        List<FlipV2> flips = new ArrayList<>();
        for (int i =0; i< number; i++) {
            FlipV2 f = new FlipV2();
            // FIX: Convert UUID.randomUUID() to String for FlipV2.id
            f.setId(UUID.randomUUID().toString()); // Line 210
            f.setAccountId(accountIds.get(new Random().nextInt(accountIds.size())));
            // leave a small percentage as open flips
            if(randomIntBetween(0, 1000) > 2) {
                f.setClosedTime(randomIntBetween(start, end));
                f.setSpent(randomIntBetween(100, 1_000_000_000));
                f.setProfit(randomIntBetween(-2_000_000, 4_000_000));
                flips.add(f);
            }
        }
        return flips;
    }

    private int randomIntBetween(int min, int max) {
        return (int)(Math.random() * ((max - min) + 1)) + min;
    }
}
