# Default finance feeds

FinanceReader adds the following twelve finance RSS feeds on first launch. This is a useful starting set, not an investment portfolio or an endorsement of any publisher, viewpoint, or content quality.

## Selection rationale

The defaults combine four kinds of coverage:

- **General and market news:** CNBC Top News, CNBC Markets, MarketWatch Top Stories, MarketWatch Market Pulse, Yahoo Finance, WSJ Markets, and Nasdaq Markets.
- **Economy and business:** NYT Economy, Fortune, and NPR Business.
- **Investment media and macro context:** Seeking Alpha and the FRED Blog.

The list favors publicly subscribable feeds, different editorial or data perspectives, and complementary coverage of market movement and macro context. Availability changes with publishers; the project does not promise that every feed will remain valid or provide full article text.

## Current default URLs

The entries are listed vertically so the long URLs remain readable on narrow screens:

- **CNBC Top News** — general finance news — <https://www.cnbc.com/id/100003114/device/rss/rss.html>
- **CNBC Markets** — market news — <https://www.cnbc.com/id/10000664/device/rss/rss.html>
- **MarketWatch Top Stories** — general market news — <https://feeds.content.dowjones.io/public/rss/mw_topstories>
- **MarketWatch Market Pulse** — market movement — <https://feeds.content.dowjones.io/public/rss/mw_marketpulse>
- **Yahoo Finance** — finance news — <https://finance.yahoo.com/news/rssindex>
- **WSJ Markets** — market news — <https://feeds.a.dj.com/rss/RSSMarketsMain.xml>
- **Nasdaq Markets** — market news — <https://www.nasdaq.com/feed/rssoutbound?category=Markets>
- **NYT Economy** — economy news — <https://rss.nytimes.com/services/xml/rss/nyt/Economy.xml>
- **Fortune** — business and companies — <https://fortune.com/feed/>
- **Seeking Alpha** — investment-market media — <https://seekingalpha.com/feed.xml>
- **NPR Business** — business and economy — <https://feeds.npr.org/1006/rss.xml>
- **FRED Blog** — macro data and explanations — <https://fredblog.stlouisfed.org/feed/>

These URLs match the current default configuration in the app. If a publisher changes an endpoint, a future app version may update the default; feeds already added to a device are not silently replaced.

## Replace or remove a feed

The defaults are only a starting point:

1. Remove unwanted feeds or use **Add feed** to add an RSS, Atom, or JSON Feed.
2. Use **OPML import/export** to move subscriptions between devices.
3. If a feed fails, open its URL in a browser to check whether the publisher still provides it. Remove the old entry and add the publisher's current URL when needed.

Article copyright and feed terms remain with their publishers. FinanceReader stores and displays the feeds selected by the user and only sends selected text to a configured translation service when the user enables that option.
