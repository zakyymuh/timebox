# Papacti sales: design

## Intent

Hobby selling, not a shop. The owner wants to know three things: **what left the collection, which plant it came from, and how much it brought in.** Pups, cuttings and seedlings get sold as well as whole plants, so one plant can have many sales.

## Goals

- Mark a plant in the collection as **for sale** with an asking price. A SALE banner and a price tag show on its photo wherever it appears in the collection.
- **Record a sale:** what went out (whole plant, pup, cutting or seedling), how many, the final price, and the date.
- A **💲 Sales tile** on the dashboard opens a **Sales page** with:
  - totals at the top: number of items sold and rupiah earned, all-time and this month
  - a **For sale** tab: cards with the sale border, photo, name and asking price
  - a **Sold** tab: every sale, newest first, with plant, kind, quantity, price and date

## Non-goals (for now)

Booked/reserved status, buyer contacts, profit and cost tracking, shipping, stock levels, and a share card with the price on it (the share card can come later and on its own).

## Anti-goals

- It must not feel like bookkeeping: recording a sale takes about three taps and one number.
- Selling the whole plant must never delete it. Its photos, notes and family tree stay.

## Approaches considered

1. **Sold as a status on the plant.** Simplest, but it can't record a pup sold while the mother stays. Rejected.
2. **A sale is its own record linked to a plant (chosen).** One plant, many sales. A "whole plant" sale also marks the plant as gone.
3. **A separate sales inventory not tied to the collection.** Loses "which one from collections", so rejected.

## Data (Room v14 → v15, migration keeps every row)

- `dex` gains:
  - `forSale` BOOLEAN default 0
  - `askingPrice` INTEGER nullable, in rupiah
  - `soldOn` TEXT nullable (LocalDate), set when a whole-plant sale is recorded
- New table `sale`:
  - `id`
  - `dexId`: FK → dex, `SET_NULL` on delete
  - `plantName`: a copy, so the record survives the plant being deleted
  - `kind`: WHOLE | PUP | CUTTING | SEEDLING
  - `quantity` INT, default 1
  - `price` INTEGER: the total for this sale, in rupiah
  - `soldOn` DATE
  - `note` TEXT, default empty
  - `createdAt`

  This follows the pattern `DexOriginEntity` uses (names copied, ids nullable).
- Pure logic goes in `core/.../collection/Sales.kt` and is unit-tested: totals (all-time, this month), the "sold" line text, and price formatting through the existing `Dex.price` style.

## Behaviour

- **Plant page:** a "Sell" action opens a sheet with a *For sale* switch, an asking price (reusing `PriceField`), and a "Record a sale" button.
- **Record a sale sheet:** pick a kind (four chips, WHOLE preselected when the plant is for sale), set a quantity (stepper, default 1), enter the price (prefilled from the asking price × quantity), pick a date (default today), and add an optional note.
  - Saving a **WHOLE** sale sets `soldOn`, clears `forSale`, and the plant leaves the collection grid and the owned count.
  - Saving a pup, cutting or seedling sale leaves the plant as it is and keeps it for sale.
- **Sold plants:** reachable from the Sold tab. Their page still opens read-only, with a "Sold on …" line.
- **Collection grid:** for-sale plants get the banner and price tag, and a "For sale" filter chip.
- A sale can be edited or deleted from the Sold tab with a long-press. Deleting a WHOLE sale puts the plant back in the collection.
- **Dashboard:** a square `Tile` with 💲, label "Sales", and the month's total as its title, or "Nothing sold yet" until there is a sale. It pairs with the History tile, or sits in a new row.

## Builder's calls (the owner can override)

- The banner style: a diagonal ribbon in the dusty rose palette, with the price in a white pill.
- Rupiah only, digits only, the same as wishlist prices.
- Seedlings that already have their own entry (from a cross) are sold as WHOLE sales of that entry. The SEEDLING kind is for unlisted seedlings sold off a mother plant.
- Where exactly the Sales tile sits on the dashboard.
