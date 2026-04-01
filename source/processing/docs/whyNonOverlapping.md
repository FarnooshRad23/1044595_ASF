**Non-overlapping**: You read characters 1-128, analyze them, then move the magnifying glass to characters 129-256, analyze those, then 257-384, and so on. 
Each batch of 128 is independent. You never re-read any character. Simple, clean, one analysis every 6.4 seconds.

**Overlapping**: You read characters 1-128 and analyze. Then you slide the magnifying glass by just one character: now you're reading 2-129 and analyzing again. Then 3-130. You're re-analyzing almost the same data with one tiny change each time. You get a result 20 times per second, but you're doing 128x more work, and most of those results will be nearly identical.

## Why non-overlapping is better in this project
The exam asks to detect events (earthquakes, explosions). An earthquake lasts several seconds.
With non-overlapping windows, you check every 6.4 seconds — more than fast enough to catch any event. 
With overlapping, you'd check 20 times per second but get the same answer 20 times, generating 20 near-duplicate inserts hitting the database.
More CPU, more duplicates, more complexity, zero benefit.

The only case where overlapping helps is if you need to detect extremely short events (less than 6.4 seconds) with sub-second precision.
The simulator's events last 5-8 seconds, so non-overlapping catches them just fine.