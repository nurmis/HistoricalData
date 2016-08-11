/**
 * Created by Android-automation on 7.6.2016.
 */
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import junit.runner.BaseTestRunner;

import org.junit.runner.JUnitCore;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class FirefoxTests extends TestCase {
    private WebDriver driver;

    public static void main(String[] args) throws Exception {
        JUnitCore.main(
                "FirefoxTests");
    }

    public void setUp() throws Exception {
        this.driver = new FirefoxDriver();
    }

    public void testThatFeaturedThemesExistOnTheHome() {
        driver.get("https://addons.allizom.org/en-US/firefox/");

        WebElement title = driver.findElement(By.cssSelector("#featured-themes h2"));
        assertEquals(title.getText(), "Featured Themes See all �");
    }

    public void testAddonsAuthorLink(){
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement addon = driver.findElement(By.cssSelector("#featured-extensions > ul > section:nth-child(1) > li:nth-child(1) > div > div.summary > a > h3"));
        WebElement authorElement = driver.findElement(By.cssSelector("#featured-extensions > ul > section:nth-child(1) > li:nth-child(1) > div > div.more > div.byline > a"));
        String authorName = authorElement.getAttribute("title");

        Actions actionChain = new Actions(driver);
        actionChain.moveToElement(addon).perform();
        actionChain.click(authorElement).perform();

        WebElement userpageElement = driver.findElement(By.cssSelector("#breadcrumbs > ol > li:nth-child(2)"));
        WebElement userpageAuthorName = userpageElement.findElement(By.tagName("span"));

        assertTrue(driver.getCurrentUrl().contains("user"));
        assertEquals(authorName, userpageAuthorName.getText());
    }

    public void testThatChecksIfTheExtensionsAreSortedByMostUser(){
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.cssSelector("#featured-extensions > h2 > a")).click();
        driver.findElement(By.cssSelector("#sorter > ul > li:nth-child(2) > a")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("#page > section.primary > h1"), "Most Popular Extensions"));
        List<Integer> counts = new ArrayList<Integer>();
        List<WebElement> extensions = driver.findElements(By.cssSelector("div.adu"));
        for (int i = 0; i < extensions.size(); i++) {
            String count = (extensions.get(i).getText().replace(",", "").replace(" users", ""));
            counts.add(Integer.parseInt(count));
        }
        List<Integer> cmprCounts = counts;
        Collections.sort(cmprCounts, Collections.reverseOrder());
        assertTrue(driver.getCurrentUrl().contains("sort=user"));
        assertEquals(counts, cmprCounts);
    }

    public void testThatChecksIfTheSubscribeLinkExists() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.cssSelector("#featured-extensions > h2 > a")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#subscribe")));
        WebElement subscribeElement = driver.findElement(By.cssSelector("#subscribe"));
        assertTrue(subscribeElement.getText().contains("Subscribe"));
    }

    public void testFeaturedTabIsHighlightedByDefault() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.cssSelector("#featured-collections > h2 > a")).click();
        WebElement defaultSelectedTab = driver.findElement(By.cssSelector("#sorter li.selected"));
        assertTrue(defaultSelectedTab.getText().contains("Featured"));
    }

    public void testCreateAndDeleteCollection() throws InterruptedException {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.xpath("//*[@id='aux-nav']/ul/li[1]/a[2]")).click();
        driver.findElement(By.id("id_username")).sendKeys("username");
        driver.findElement(By.id("id_password")).sendKeys("password" + Keys.RETURN);

        driver.findElement(By.cssSelector("#collections > a")).click();
        driver.findElement(By.cssSelector("#side-nav > section:nth-child(4) > p:nth-child(3) > a")).click();

        String colUUID = UUID.randomUUID().toString();
        Long collectionTime = System.currentTimeMillis();
        String collectionName =  colUUID.substring(0, 30 - (collectionTime.toString().length())) + collectionTime;

        driver.findElement(By.id("id_name")).sendKeys(collectionName);
        Thread.sleep(1000);
        driver.findElement(By.id("id_description")).sendKeys(collectionName);
        driver.findElement(By.cssSelector("#main-wrapper > div.section > div.primary > div > div > form > p:nth-child(6) > input[type='submit']")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='main-wrapper']/div[1]/div[3]/h2")));
        assertEquals(driver.findElement(By.cssSelector(".notification-box.success h2")).getText(), "Collection created!");
        assertEquals(driver.findElement(By.cssSelector(".collection > span")).getText(), collectionName);

        driver.findElement(By.cssSelector(".delete")).click();
        driver.findElement(By.cssSelector(".section > form > button")).click();
        List<WebElement> collections = driver.findElements(By.cssSelector(".featured-inner div.item"));

        if (collections.isEmpty()){
            return;
        } else {
            for (int i = 0; i < collections.size(); i++) {
                assertTrue(collectionName != collections.get(i).getText());
            }
        }
    }

    public void testThatClickingTheAmoLogoLoadsHomePage() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement amoLogo = driver.findElement(By.cssSelector(".site-title"));
        assertTrue(amoLogo.isDisplayed());
        amoLogo.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement titleElement = driver.findElement(By.cssSelector(".site-title"));
        assertTrue(titleElement.isDisplayed());
        assertEquals(driver.getCurrentUrl(), "https://addons.allizom.org/en-US/firefox/");
    }

    public void testThatOtherApplicationsLinkHasTooltip() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement otherApplicationsElement = driver.findElement(By.id("other-apps"));
        String tooltip = otherApplicationsElement.getAttribute("title");
        assertEquals(tooltip, "Find add-ons for other applications");
    }

    public void testTheSearchBoxExist() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement searchTextBox = driver.findElement(By.id("search-q"));
        assertTrue(searchTextBox.isDisplayed());
    }

    public void testThatNewReviewIsSaved() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.xpath("//*[@id='aux-nav']/ul/li[1]/a[2]")).click();
        driver.findElement(By.id("id_username")).sendKeys("username");
        driver.findElement(By.id("id_password")).sendKeys("password" + Keys.RETURN);

        WebElement userLoggedIn = driver.findElement(By.cssSelector("#aux-nav .account a.user"));
        assertEquals(driver.getCurrentUrl(), "https://addons.allizom.org/en-US/firefox/");
        assertTrue(userLoggedIn.isDisplayed());

        driver.get("https://addons.allizom.org/en-US/firefox/addon/firebug/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='add-review']"))); // load review button
        driver.findElement(By.xpath("//*[@id='add-review']")).click();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSSSS").format(Calendar.getInstance().getTime());
        String body = "Automatic addon review by Selenium tests " + timeStamp;
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("review-box"))); // load popup box
        driver.findElement(By.id("id_review_body")).sendKeys(body);
        WebElement ratingElement = driver.findElement(By.cssSelector(".ratingwidget.stars.stars-0 > label"));
        Actions actionChain = new Actions(driver);
        actionChain.moveToElement(ratingElement).click().perform();
        driver.findElement(By.cssSelector("#review-box input[type=submit]")).click();

        driver.findElement(By.cssSelector("#aux-nav > ul > li.account > a")).click(); // click myprofile

        String timeStampToday = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        List<WebElement> reviews = driver.findElements(By.cssSelector("#reviews > div"));
        WebElement recentReview = reviews.get(0);
        assertTrue(recentReview.findElement(By.cssSelector("span.stars")).getText().contains("1"));
        assertTrue(recentReview.getText().contains(timeStampToday));

        driver.findElement(By.cssSelector(".delete-review")).click(); // click delete review
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".item-actions > li:nth-child(2)"), "Marked for deletion")); //confirm review is marked for deletion
    }

    public void testThatSearchingForCoolReturnsResultsWithCoolInTheirNameDescription() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        String searchTerm = "cool";
        driver.findElement(By.id("search-q")).sendKeys(searchTerm);
        driver.findElement(By.cssSelector("#search > button")).click();

        List<WebElement> noResultElement = driver.findElements(By.cssSelector("p.no-results"));
        assertTrue(noResultElement.isEmpty());

        List<WebElement> searchResults = driver.findElements(By.cssSelector("div.items div.item.addon"));
        for (int i = 0; i < searchResults.size(); i++) {
            try {
                assertTrue(searchResults.get(i).getText().toLowerCase().contains(searchTerm));
            } catch (AssertionFailedError e) { //if you want to run this try football as search term
                searchResults.get(i).findElement(By.cssSelector("div.info > h3 > a")).click();
                List<WebElement> comments = driver.findElements(By.cssSelector("#developer-comments"));
                String devComments;
                if (comments.size() != 0); {
                    driver.findElement(By.cssSelector("#developer-comments h2 a")).click();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#developer-comments div.content")));
                    devComments = driver.findElement(By.cssSelector("#developer-comments div.content")).getText();
                }
                String searchRange = driver.findElement(By.cssSelector("div.prose")).getText() + devComments;
                assertTrue(searchRange.toLowerCase().contains(searchTerm));
                driver.navigate().back();
                searchResults = driver.findElements(By.cssSelector("div.items div.item.addon")); //Because link has been clicked, the elementlist needs to be refreshed
            }
        }
    }

    public void testSortingByNewest() throws ParseException {
        ArrayList<Long> dates = new ArrayList<Long>();
        SimpleDateFormat source = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        String searchTerm = "firebug";
        driver.findElement(By.id("search-q")).sendKeys(searchTerm);
        driver.findElement(By.cssSelector("#search > button")).click();

        driver.findElement(By.xpath("//div[@id='sorter']//li/a[normalize-space(text())='Newest']")).click(); //click sort by newest
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".updating.tall")));
        List<WebElement> searchResults = driver.findElements(By.cssSelector("div.items div.item.addon"));
        for (int i = 0; i < searchResults.size(); i++) {
            String date = searchResults.get(i).findElement(By.cssSelector("div.info > div.vitals > div.updated")).getText().replace("Added ", "");
            Date d = source.parse(date);
            dates.add(d.getTime());
        }
        ArrayList<Long> cmprDates = dates;
        Collections.sort(cmprDates, Collections.reverseOrder());

        assertEquals(dates, cmprDates);
        assertTrue(driver.getCurrentUrl().contains("sort=created"));
    }

    public void testThatSearchingForATagReturnsResults() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        String searchTerm = "development";
        driver.findElement(By.id("search-q")).sendKeys(searchTerm);
        driver.findElement(By.cssSelector("#search > button")).click();

        Integer countWithoutTag = driver.findElements(By.cssSelector("div.items div.item.addon")).size();
        assertTrue(countWithoutTag > 0);

        driver.findElement(By.cssSelector("li#tag-facets h3")).click();
        driver.findElement(By.cssSelector("#tag-facets > ul > li:nth-child(4) > a")).click();

        assertTrue(countWithoutTag >= driver.findElements(By.cssSelector("div.items div.item.addon")).size());
    }

    public void testThatVerifiesTheUrlOfTheStatisticsPage() {
        driver.get("https://addons.allizom.org/en-US/firefox/addon/firebug/");
        WebDriverWait wait = new WebDriverWait(driver, 10);

        driver.findElement(By.cssSelector("#daily-users > a.stats")).click();
        wait.until(ExpectedConditions.titleContains("Statistics"));

        assertTrue(driver.getCurrentUrl().contains("/statistics"));
    }

    public void testTheRecentlyAddedSection() throws ParseException {
        SimpleDateFormat source = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        ArrayList<Long> dates = new ArrayList<Long>();
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.cssSelector("#themes > a")).click();
        List<WebElement> recentlyAdded = driver.findElements(By.cssSelector("#personas-created .persona-small"));
        for (int i = 0; i < recentlyAdded.size(); i++) {
            Date d = source.parse(recentlyAdded.get(i).getText().replace("Added ", ""));
            dates.add(d.getTime());
        }
        ArrayList<Long> cmprDates = dates;
        Collections.sort(cmprDates, Collections.reverseOrder());

        assertTrue(driver.getCurrentUrl().contains("themes"));
        assertEquals(6, recentlyAdded.size());
        assertEquals(dates, cmprDates);
    }

    public void testThatMostPopularLinkIsDefault() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        WebElement themesMenu = driver.findElement(By.cssSelector("#themes"));
        WebElement completeThemesMenu = driver.findElement(By.cssSelector("#site-nav div > a.complete-themes > b"));
        Actions actionChain = new Actions(driver);
        actionChain.moveToElement(themesMenu).moveToElement(completeThemesMenu).click().perform();
        List<WebElement> links = driver.findElements(By.cssSelector("#side-explore a"));
        String exploreFilter = "";
        for (int i = 0; i < links.size(); i++) {
            String linkExamined = links.get(i).getCssValue("font-weight");
            int link = 0;
            try {
                link = Integer.parseInt(linkExamined);
            } catch (NumberFormatException e){
                if (linkExamined.contains("bold")){
                    exploreFilter = links.get(i).getText();
                }
            }
            if (link > 400) {
                exploreFilter = links.get(i).getText();
            }
        }

        assertTrue(driver.getCurrentUrl().contains("themes/"));
        assertEquals(exploreFilter, "Most Popular");
    }
    public void testThatExternalLinkLeadsToAddonWebsite() {
        driver.get("https://addons.allizom.org/en-US/firefox/addon/memchaser/");

        String websiteLink = driver.findElement(By.cssSelector(".links a.home")).getAttribute("href");
        String[] parts = websiteLink.split("https");
        String result = parts[2].substring(3);
        assertTrue(result != "");

        driver.findElement(By.cssSelector(".links a.home")).click();
        assertTrue(driver.getCurrentUrl().contains(result));
    }

    public void testThatUserCamAccessTheEditProfilePage() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.xpath("//*[@id='aux-nav']/ul/li[1]/a[2]")).click();
        driver.findElement(By.id("id_username")).sendKeys("username");
        driver.findElement(By.id("id_password")).sendKeys("password" + Keys.RETURN);
        WebElement userLoggedInElement = driver.findElement(By.cssSelector("#aux-nav .account a.user"));
        assertTrue(driver.getCurrentUrl().contains("https://addons.allizom.org/en-US/firefox/"));
        assertTrue(userLoggedInElement.isDisplayed());

        WebElement hoverProfile = driver.findElement(By.cssSelector("#aux-nav .account a.user"));
        WebElement clickProfile = driver.findElement(By.cssSelector("#aux-nav .account ul")).findElement(By.cssSelector(" li:nth-child(2) a"));
        Actions actionChain = new Actions(driver);
        actionChain.moveToElement(hoverProfile).moveToElement(clickProfile).click().perform();

        assertTrue(driver.getCurrentUrl().contains("/users/edit"));
        assertEquals("My Account", driver.findElement(By.cssSelector("#acct-account > legend")).getText());
        assertEquals("Profile", driver.findElement(By.cssSelector("#profile-personal > legend")).getText());
        assertEquals("Details", driver.findElement(By.cssSelector("#profile-detail > legend")).getText());
        assertEquals("Notifications", driver.findElement(By.cssSelector("#acct-notify > legend")).getText());
    }

    public void testThatMakeContributionButtonIsClickableWhileUserIsLoggedIn() {
        driver.get("https://addons.allizom.org/en-US/firefox/addon/firebug/");

        driver.findElement(By.xpath("//*[@id='aux-nav']/ul/li[1]/a[2]")).click();
        driver.findElement(By.id("id_username")).sendKeys("username");
        driver.findElement(By.id("id_password")).sendKeys("password" + Keys.RETURN);
        WebElement userLoggedInElement = driver.findElement(By.cssSelector("#aux-nav .account a.user"));
        assertTrue(userLoggedInElement.isDisplayed());

        driver.findElement(By.cssSelector("#contribute-button")).click();
        assertTrue(driver.findElement(By.id("contribute-confirm")).isDisplayed());
        assertEquals("Make Contribution", driver.findElement(By.id("contribute-confirm")).getText());
    }

    public void testTheLogoutLinkForLoggedInUsers() {
        driver.get("https://addons.allizom.org/en-US/firefox/");
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='promos']"))); //load promomenu

        driver.findElement(By.xpath("//*[@id='aux-nav']/ul/li[1]/a[2]")).click();
        driver.findElement(By.id("id_username")).sendKeys("username");
        driver.findElement(By.id("id_password")).sendKeys("password" + Keys.RETURN);
        WebElement userLoggedInElement = driver.findElement(By.cssSelector("#aux-nav .account a.user"));
        assertTrue(driver.getCurrentUrl().contains("https://addons.allizom.org/en-US/firefox/"));
        assertTrue(userLoggedInElement.isDisplayed());

        WebElement discoveryPane = driver.findElement(By.cssSelector("#aux-nav > ul > li.account > a"));
        WebElement logoutButton = driver.findElement(By.cssSelector("#aux-nav > ul > li.account > ul > li.nomenu.logout > a"));
        Actions actionChain = new Actions(driver);
        actionChain.moveToElement(discoveryPane).moveToElement(logoutButton).click().perform();

        assertTrue(driver.getCurrentUrl().contains("https://addons.allizom.org/en-US/firefox/"));
        List<WebElement> userLogged = driver.findElements(By.cssSelector("#aux-nav .account a.user"));
        assertTrue(userLogged.size() == 0);
    }

    public void tearDown() throws Exception {
        this.driver.quit();
    }

}
