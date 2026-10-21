package ridehailing.app;

import ridehailing.driver.CityMapPoint;
import ridehailing.driver.Driver;
import ridehailing.strategy.TaxiClass;
import ridehailing.strategy.matching.DriverMatchingContext;
import ridehailing.strategy.matching.DriverMatchingStrategy;
import ridehailing.strategy.matching.DriverMatchingStrategySelector;
import ridehailing.strategy.matching.DriverStatsSource;
import ridehailing.strategy.matching.HighestRatedDriverStrategy;
import ridehailing.strategy.matching.LongestIdleDriverStrategy;
import ridehailing.strategy.matching.NearestDriverStrategy;
import ridehailing.strategy.matching.SimulatedDriverStatsSource;
import ridehailing.strategy.pricing.DemandSource;
import ridehailing.strategy.pricing.PremiumPricingStrategy;
import ridehailing.strategy.pricing.PriceInput;
import ridehailing.strategy.pricing.PricingContext;
import ridehailing.strategy.pricing.PricingStrategy;
import ridehailing.strategy.pricing.PricingStrategySelector;
import ridehailing.strategy.pricing.SimulatedDemandSource;
import ridehailing.strategy.pricing.StandardPricingStrategy;
import ridehailing.strategy.pricing.SurgePricingStrategy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/** Console demo of Assignment 5: strategies, manual choice, automatic choice, switching at runtime. */
public class StrategyDemo {

    public static void main(String[] args) {
        DemandSource calmCityDemand = new SimulatedDemandSource(clockAtHour(13));
        DemandSource rushHourDemand = new SimulatedDemandSource(clockAtHour(18));

        showSamePriceInputInAllPricingStrategies(rushHourDemand);
        showManualAndAutomaticPricing(calmCityDemand, rushHourDemand);
        showSwitchingPricingStrategyAtRuntime(calmCityDemand, rushHourDemand);
        showDriverMatching();
    }

    private static void showSamePriceInputInAllPricingStrategies(DemandSource demandSource) {
        System.out.println("=== 1. One ride, every pricing strategy (rush hour) ===");
        PriceInput ride = new PriceInput(8, "CENTER", TaxiClass.REGULAR);
        PricingContext pricingContext = new PricingContext(new StandardPricingStrategy());
        List<PricingStrategy> allStrategies = List.of(
                new StandardPricingStrategy(), new SurgePricingStrategy(demandSource), new PremiumPricingStrategy());
        for (PricingStrategy strategy : allStrategies) {
            pricingContext.setStrategy(strategy);
            System.out.println(strategy.getClass().getSimpleName() + ": " + pricingContext.calculatePrice(ride));
        }
    }

    private static void showManualAndAutomaticPricing(DemandSource calmCityDemand, DemandSource rushHourDemand) {
        System.out.println("\n=== 2. Manual choice and automatic choice (Selector) ===");
        PriceInput regularRide = new PriceInput(8, "CENTER", TaxiClass.REGULAR);
        PriceInput premiumRide = new PriceInput(8, "CENTER", TaxiClass.PREMIUM);

        PricingContext manualContext = new PricingContext(new PremiumPricingStrategy());
        System.out.println("Manual (we decided Premium): " + manualContext.calculatePrice(regularRide));

        printAutomaticChoice("Regular taxi, calm city", new PricingStrategySelector(calmCityDemand), regularRide);
        printAutomaticChoice("Regular taxi, rush hour", new PricingStrategySelector(rushHourDemand), regularRide);
        printAutomaticChoice("Premium taxi, rush hour", new PricingStrategySelector(rushHourDemand), premiumRide);
    }

    private static void printAutomaticChoice(String situation, PricingStrategySelector selector, PriceInput ride) {
        PricingStrategy chosenStrategy = selector.chooseStrategyFor(ride);
        double price = new PricingContext(chosenStrategy).calculatePrice(ride);
        System.out.println("Auto, " + situation + " -> " + chosenStrategy.getClass().getSimpleName() + ": " + price);
    }

    private static void showSwitchingPricingStrategyAtRuntime(DemandSource calmCityDemand, DemandSource rushHourDemand) {
        System.out.println("\n=== 3. Switching the strategy while the program runs ===");
        PriceInput ride = new PriceInput(8, "CENTER", TaxiClass.REGULAR);
        PricingContext pricingContext = new PricingContext(new PricingStrategySelector(calmCityDemand).chooseStrategyFor(ride));
        System.out.println("13:00 -> " + pricingContext.calculatePrice(ride));

        pricingContext.setStrategy(new PricingStrategySelector(rushHourDemand).chooseStrategyFor(ride));
        System.out.println("18:00, same context, new strategy -> " + pricingContext.calculatePrice(ride));
    }

    private static void showDriverMatching() {
        System.out.println("\n=== 4. Driver matching ===");
        List<Driver> drivers = List.of(
                new Driver("D1", "Aidar", new CityMapPoint(1, 1)),
                new Driver("D2", "Bauyrzhan", new CityMapPoint(6, 2)),
                new Driver("D3", "Dana", new CityMapPoint(3, 8)));
        CityMapPoint pickupPoint = new CityMapPoint(0, 0);
        DriverStatsSource driverStatsSource = new SimulatedDriverStatsSource();

        DriverMatchingContext matchingContext = new DriverMatchingContext(new NearestDriverStrategy());
        List<DriverMatchingStrategy> allStrategies = List.of(
                new NearestDriverStrategy(),
                new HighestRatedDriverStrategy(driverStatsSource),
                new LongestIdleDriverStrategy(driverStatsSource));
        for (DriverMatchingStrategy strategy : allStrategies) {
            matchingContext.setStrategy(strategy);
            System.out.println(strategy.getClass().getSimpleName() + ": "
                    + matchingContext.chooseDriver(drivers, pickupPoint).getDriverName());
        }

        DriverMatchingStrategySelector selector = new DriverMatchingStrategySelector(driverStatsSource);
        printAutomaticMatching("Premium taxi, 14:00", selector, TaxiClass.PREMIUM, LocalTime.of(14, 0), drivers, pickupPoint);
        printAutomaticMatching("Regular taxi, 14:00", selector, TaxiClass.REGULAR, LocalTime.of(14, 0), drivers, pickupPoint);
        printAutomaticMatching("Regular taxi, 23:30", selector, TaxiClass.REGULAR, LocalTime.of(23, 30), drivers, pickupPoint);
    }

    private static void printAutomaticMatching(String situation, DriverMatchingStrategySelector selector,
                                               TaxiClass taxiClass, LocalTime timeOfOrder,
                                               List<Driver> drivers, CityMapPoint pickupPoint) {
        DriverMatchingStrategy chosenStrategy = selector.chooseStrategyFor(taxiClass, timeOfOrder);
        String driverName = new DriverMatchingContext(chosenStrategy).chooseDriver(drivers, pickupPoint).getDriverName();
        System.out.println("Auto, " + situation + " -> " + chosenStrategy.getClass().getSimpleName() + ": " + driverName);
    }

    private static Clock clockAtHour(int hour) {
        return Clock.fixed(Instant.parse(String.format("2026-01-01T%02d:00:00Z", hour)), ZoneOffset.UTC);
    }
}
