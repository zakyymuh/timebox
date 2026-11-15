# Papacti sales, round 2: design

Builds on the shipped sales feature (`2026-11-15-papacti-sales-design.md`, DB v15, `SaleRepository`, `ui/sales/`). The owner approved this round from a mockup: https://claude.ai/artifact/AKpQ3d5TEJUJqwvPLE7mVa (5 screens).

## Why

The owner only ever sells **whole plants**. Selling should feel like moving a plant from the collection onto a shelf, then marking it sold. It shouldn't feel like filling in a form. Sales get posted as Instagram Stories, so every share image should be a story.

## The flow

Collection → plant page → **$** (asking price, optional) → the plant leaves the collection grid and shows in **Sales › For sale** → tap its card → its page → **Sold** (optional negotiated price) → it moves to **Sold**. Or **Cancel sale** puts it back in the collection.

## Changes

### 1. Plant page in the collection (`DexDetailScreen`, plant in the collection, not for sale)
- Remove the full-width "💲 Sell" button and the `SellSheet` switch dialog.
- Add a round **$** icon button, 56dp, rose outline, ink "$", on the **same row as Repot and Breed**. Repot and Breed keep `weight(1f)`; the $ is a fixed circle. Content description: "Put up for sale".
- Tapping $ opens a small dialog: "Put Astro up for sale", an optional **Asking price** field (`RupiahField`), then Cancel / **Put up for sale**. Confirming calls `setForSale(true, asking)`, shows a toast ("Astro is up for sale 💲") and goes back (the plant is no longer in the collection grid).

### 2. Collection grid (`CollectionScreen`)
- A for-sale plant **leaves the grid**: show `inCollection && !forSale`. The tab count matches.
- Remove the "For sale · n" chip and `forSaleOnly`.
- The dashboard plant count and the breeding parent picker **keep** for-sale plants, since you still own them. Only the grid hides them.
- The SALE ribbon is no longer used anywhere. Delete `SaleRibbon` if nothing else uses it. Keep `Modifier.saleBorder`.

### 3. Sales › For sale cards (`SalesScreen`)
- A card has the rose border (`saleBorder`) and the photo with a white **price pill** at the bottom-left (`Money.rupiah(askingPrice)`, no pill without a price). There is **no ribbon**.
- A **round share button** (44dp white circle, rose stroke share icon, light shadow) sits **inside the photo, top-right**. It shares that plant as a story (section 6) with its price. It must not also open the card.
- Tapping the card opens the plant's page (section 4).

### 4. A for-sale plant's page (`DexDetailScreen` when `entry.forSale`)
- It looks like the collection detail: photo diary, name, species, notes. The photo gets the rose border and the round share button inside it, top-right.
- Hide the **Edit** pill, Repot, Breed and $.
- Add an **Asking price** card (pastel, rose text): "Rp 150.000", plus a small line "For sale since …". Tapping the card edits the asking price in the same dialog as section 1. "For sale since" needs a date: add `forSaleSince: LocalDate?` to `dex`, which means a DB v16 migration with a nullable TEXT column, set when `setForSale(true, …)` turns it on. Builder: if you'd rather skip the migration, drop the "since" line and say so.
- At the bottom:
  - **Sold**: full-width, filled ink, 56dp.
  - Under it, **Cancel sale**: muted grey text (`Fresh.InkSoft`, 14sp, no border or background). It is still a real `TextButton` with a 44dp touch target. Tapping it calls `setForSale(false, asking)`, toasts "Back in your collection", and stays on the page, which now shows as a normal collection plant.

### 5. The Sold pop-up
- Title "Sold Astro?", a line "Asking price Rp 150.000", an optional **Negotiated price** field (placeholder = the asking price), a hint "Leave it empty if they paid the asking price. Sold today.", then Cancel / **Sold**.
- Saving records a **WHOLE** sale: `price = negotiated ?: asking ?: 0`, `soldOn = today`, note empty.
- Tapping Sold with no asking price and no negotiated price is blocked: the Sold button stays disabled until a price is typed.
- After saving: a toast "Sold! 💲" and navigate back to Sales. The plant now shows under **Sold**.

### 6. Every share is an Instagram Story (`ShareCard`)
- Change the canvas from **1080×1350 to 1080×1920 (9:16)** for `sharePlant`, `shareCross` and the new sale share.
- The plant photo grows into the tall space: a portrait box, full width inside `MARGIN`, ending about 330px above the bottom. Crop the photo to fill (centre-crop).
- The title, subtitle and gyotaku rosette print keep their style, and the print stays half on the photo's lower-right corner.
- The cross layout grows the same way: the two photos sit side by side, taller, with the × on the seam.
- Add `sharePlantForSale(context, file, title, price: Long?, subtitle)`: the same as `sharePlant`, plus a rose bold price line between the title and the subtitle. **No SALE text or watermark.**

### 7. Record UI is whole-plant only
- Remove the kind chips and quantity stepper from `RecordSaleSheet`. It is now only used for the long-press edit in the Sold tab: Sold for, Sold on, note, Delete.
- Keep the `SaleKind` enum, the `kind`/`quantity` columns and `Sales.line` handling of old pup and cutting rows, so existing sales still read. New sales are always WHOLE with quantity 1.
- Totals and the dashboard sales line stay as they are.

## Non-goals
Booked or reserved status, buyers, stock, a price history, and changing the dashboard. The 2×2 tile grid belongs to another session. Leave `DashboardScreen.kt` alone.

## Tests the builder must add
- Core:
  - the grid filter: a for-sale plant is excluded from the grid but still counted as owned
  - the negotiated price falls back to the asking price
  - the share story is 1080×1920, if the size is a pure constant you can expose
- App (Robolectric):
  - put up for sale → the plant leaves `CollectionViewModel`'s grid list and appears in `SalesViewModel.forSale`
  - Sold with a negotiated price → one WHOLE sale at that price, and the plant is in Sold
  - Sold with an empty price → a sale at the asking price
  - Cancel sale → back in the grid, not in For sale
  - the v16 migration keeps rows, if you add `forSaleSince`

## Constraints
- Rupiah only, digits only, `0..DexForm.MAX_PRICE`.
- Never delete a plant on sale.
- Check `ListAgents` and `git status` before committing. Session `timebox-9a` edits `DashboardScreen.kt`, `SoilWaterCard.kt` and `LightCard.kt` on `plant-lineage`. Work on a branch in its own worktree.
- This Mac can't run the Android tests (no SDK, JDK 17). `./gradlew :core:test` runs locally, and the app tests run on CI when the branch is pushed.
