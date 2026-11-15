# Papacti Sales Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mark collection plants for sale, record sales (whole plant, pup, cutting or seedling), and see what went out and for how much on a Sales page reached from a 💲 dashboard tile.

**Architecture:** Pure rules (kinds, totals, text) live in `core/.../collection/Sales.kt`. Room gets three `dex` columns and a `sale` table (v14 → v15). A new `SaleRepository` owns the side effects of a whole-plant sale. The UI adds a ribbon to `DexCard`, a Sell sheet on `DexDetailScreen`, a new `ui/sales/` screen, and a dashboard tile.

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit4, and Robolectric (sdk 33) for app tests.

**Spec:** `docs/superpowers/specs/2026-11-15-papacti-sales-design.md`

## Global Constraints

- Prices are rupiah only, digits only, valid range `0..DexForm.MAX_PRICE` (999_999_999), formatted with `Money.rupiah` ("Rp 150.000").
- DB version 15. The migration keeps every row. Append `MIGRATION_14_15` to `ALL_MIGRATIONS`.
- Selling the whole plant never deletes the `dex` row, its photos or its `dex_origin`.
- A sale copies `plantName`, so the record survives the plant being deleted (`dexId` is `SET_NULL`).
- Kind labels (exact copy): `Whole plant`, `Pup`, `Cutting`, `Seedling`.
- Leave out booked status, buyer contacts, profit and the price share card.
- **Parallel work:** session `timebox-9a`'s light check (`8bbc7bc`: `ui/light/` plus edits to `AppNav.kt`, `Destinations.kt` and `DashboardScreen.kt`) is pushed to `origin/plant-lineage`. Build in a worktree on branch `papacti-sales`, cut from `origin/plant-lineage` at or after `8bbc7bc`. Before Tasks 5 and 6 (which touch those files), run `ListAgents` and `git fetch`, and rebase if `plant-lineage` moved.
- Tests: `./gradlew :core:test :app:testDebugUnitTest`.

## Review Focus

1. **Deleting a plant that has sales:** the Sold tab still lists those sales by the copied name, and tapping one does nothing (no crash on a null `dexId`). Test in Task 3.
2. **Deleting a WHOLE sale, or editing it to another kind:** `soldOn` clears and the plant comes back to the collection count. Test in Task 3.
3. **Recording a second WHOLE sale for an already-sold plant:** refused, so one plant can't be sold twice. Test in Task 3.
4. **"This month" at a month boundary:** a sale on the 31st doesn't count in the next month. It uses the sale's `soldOn` date, not `createdAt`. Test in Task 1.
5. **Sold plants leaking into lists:** a sold plant doesn't show in the collection grid, the dashboard plant count or cover, or the breeding parent picker. Covered by `DexEntry.inCollection` tests in Task 1 and the dashboard test in Task 6.

---

### Task 1: Sales rules in core

**Files:**
- Create: `core/src/main/kotlin/dev/zakyymuh/timebox/core/collection/Sales.kt`
- Modify: `core/src/main/kotlin/dev/zakyymuh/timebox/core/collection/Dex.kt` (`DexEntry`)
- Test: `core/src/test/kotlin/dev/zakyymuh/timebox/core/collection/SalesTest.kt`

**Interfaces:**
- Produces:
  - `enum class SaleKind(val label: String) { WHOLE("Whole plant"), PUP("Pup"), CUTTING("Cutting"), SEEDLING("Seedling") }`
  - `data class Sale(val id: Long, val dexId: Long?, val plantName: String, val kind: SaleKind, val quantity: Int, val price: Long, val soldOn: LocalDate, val note: String)`
  - `data class SaleForm(val kind: SaleKind = SaleKind.WHOLE, val quantity: Int = 1, val price: Long? = null, val soldOn: LocalDate, val note: String = "")` with `val isValid` (quantity 1..99, price non-null in `0..DexForm.MAX_PRICE`, note ≤ 300 chars). For WHOLE, the quantity is forced to 1 by `Sales.normalise`.
  - `data class SalesTotals(val items: Int, val earned: Long, val monthItems: Int, val monthEarned: Long)`
  - `object Sales`:
    - `fun totals(sales: List<Sale>, today: LocalDate): SalesTotals`: items is the sum of quantities, and the month is the same `YearMonth` as `today`
    - `fun line(sale: Sale): String`
    - `fun suggestedPrice(asking: Long?, quantity: Int): Long?`
    - `fun normalise(form: SaleForm): SaleForm`
  - `DexEntry` gains `forSale: Boolean = false`, `askingPrice: Long? = null`, `soldOn: LocalDate? = null`, and `val inCollection: Boolean get() = owned && soldOn == null`.

- [ ] **Step 1: Write the failing tests** in `SalesTest`:
  - `totals add quantities and prices`: `[PUP×3 Rp60.000 on 2026-11-02, WHOLE×1 Rp150.000 on 2026-10-30]`, today `2026-11-15` → `SalesTotals(4, 210_000, 3, 60_000)`
  - `a sale on the last day of last month is not this month`: a sale on `2026-10-31`, today `2026-11-01` → `monthItems == 0`
  - `no sales is all zero` → `SalesTotals(0, 0, 0, 0)`
  - `line for pups`: `Sale(kind=PUP, quantity=2, price=50_000, plantName="Astro")` → `"2 pups from Astro · Rp 50.000"`
  - `line for one cutting` → `"1 cutting from Astro · Rp 25.000"`
  - `line for whole plant` → `"Astro · Rp 150.000"`
  - `suggested price multiplies asking by quantity`: `(40_000, 3)` → `120_000`, and `(null, 3)` → `null`
  - `normalise forces whole plant to quantity 1`: `SaleForm(WHOLE, quantity=4, …)` → `quantity == 1`
  - `form needs a price`, `form rejects quantity 0`, `form rejects price above max`
  - `a sold plant is not in the collection`: `DexEntry(owned=true, soldOn=2026-11-01).inCollection == false`. An owned plant with no `soldOn` → true, and a wish → false.
- [ ] **Step 2: Run** `./gradlew :core:test --tests '*SalesTest'`. Expected: FAIL (unresolved `Sales`).
- [ ] **Step 3: Implement** `Sales.kt` and the `DexEntry` fields. `line` uses the lowercase label plus an "s" when quantity > 1 ("pups", "cuttings", "seedlings"), and the price goes through `Money.rupiah`.
- [ ] **Step 4: Run** `./gradlew :core:test`. Expected: all PASS, with the existing `Dex*Test`s unchanged.
- [ ] **Step 5: Commit** `feat(core): sales rules: kinds, totals, sale lines, sold plants leave the collection`

### Task 2: Room schema v15

**Files:**
- Modify: `app/src/main/java/dev/zakyymuh/timebox/data/DexEntities.kt` (the `DexEntity` columns and `toModel`)
- Create: `app/src/main/java/dev/zakyymuh/timebox/data/SaleData.kt` (`SaleEntity`, `SaleDao`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/data/TimeboxDatabase.kt` (add the entity, `version = 15`, `abstract fun saleDao(): SaleDao`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/data/Migrations.kt`
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/MigrationTest.kt`

**Interfaces:**
- Consumes: `SaleKind` and `Sale` from Task 1.
- Produces:
  - `DexEntity` gains `@ColumnInfo(defaultValue = "0") val forSale: Boolean = false`, `val askingPrice: Long? = null` and `val soldOn: LocalDate? = null`. `toModel()` passes them through.
  - `@Entity(tableName = "sale", foreignKeys = [ForeignKey(DexEntity, ["id"], ["dexId"], onDelete = SET_NULL)], indices = [Index("dexId")]) data class SaleEntity(id: Long = 0, dexId: Long?, plantName: String, kind: String /* SaleKind.name */, quantity: Int, price: Long, soldOn: LocalDate, note: String, createdAt: Instant)` with `toModel(): Sale`
  - `SaleDao`:
    - `salesFlow(): Flow<List<SaleEntity>>`, ordered `soldOn DESC, id DESC`
    - `sale(id): SaleEntity?`
    - `wholeSaleFor(dexId): SaleEntity?`, which finds the plant's whole-plant sale (`kind = 'WHOLE'`)
    - `insert(sale): Long`, `update(sale)`, `delete(id)`
  - `DexDao` gains `forSaleFlow(): Flow<List<DexEntity>>`: `WHERE owned = 1 AND forSale = 1 AND soldOn IS NULL ORDER BY number`
- [ ] **Step 1: Write the failing test** `the upgrade to 15 keeps each plant, not for sale, and adds an empty sale table that works`. Copy the shape of the v14 test, with a callback at 14 and migrations `≤ 14`. Insert Kikko, open the current DB, then assert `forSale == false`, `askingPrice == null`, `soldOn == null`, insert a `SaleEntity` for Kikko, and read it back through `salesFlow().first()`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*MigrationTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `MIGRATION_14_15`:
  - three `ALTER TABLE dex ADD COLUMN` statements: `forSale INTEGER NOT NULL DEFAULT 0`, `askingPrice INTEGER`, `soldOn TEXT`
  - `CREATE TABLE sale (…)` with `FOREIGN KEY(dexId) REFERENCES dex(id) ON UPDATE NO ACTION ON DELETE SET NULL`
  - `CREATE INDEX index_sale_dexId`

  The SQL must match what Room generates, the same way `MIGRATION_12_13` was written.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: all PASS, including the earlier migration tests.
- [ ] **Step 5: Commit** `feat: database v15: plants can be for sale, and a sale table`

### Task 3: SaleRepository

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/data/SaleRepository.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ServiceLocator.kt` (add `saleRepository(context)`, the same singleton pattern as `breedingRepository`)
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/SaleRepositoryTest.kt` (the same Robolectric setup and fixed clock as `DexRepositoryTest`)

**Interfaces:**
- Consumes: `SaleDao` and `DexDao.forSaleFlow` from Task 2, and `SaleForm` and `Sales.normalise` from Task 1.
- Produces: `class SaleRepository(dao: SaleDao, dexDao: DexDao, clock: Clock)` with:
  - `fun salesFlow(): Flow<List<Sale>>`
  - `fun forSaleFlow(): Flow<List<DexEntry>>`
  - `fun todayDate(): LocalDate`
  - `suspend fun setForSale(dexId: Long, forSale: Boolean, askingPrice: Long?)`, a no-op if the plant is a wish or already sold
  - `suspend fun record(dexId: Long, form: SaleForm): Long?`, which returns null when refused
  - `suspend fun edit(saleId: Long, form: SaleForm)`
  - `suspend fun delete(saleId: Long)`

  Every write runs in `db.withTransaction`, so the constructor also takes `db: TimeboxDatabase`. Each write re-derives the plant's `soldOn` from whether a WHOLE sale exists.
- [ ] **Step 1: Write the failing tests:**
  - `setting for sale stores the asking price`
  - `a wish cannot be put up for sale`
  - `selling a pup keeps the plant in the collection and for sale`
  - `selling the whole plant marks it sold on the sale date and takes it off for sale`: `soldOn == form.soldOn`, `forSale == false`, the row still exists, and `forSaleFlow` no longer lists it
  - `a sold plant cannot be sold whole again`: the second `record(WHOLE)` → `null`, with one sale stored
  - `deleting the whole sale brings the plant back`: `soldOn == null`
  - `editing a whole sale into a cutting brings the plant back`
  - `deleting the plant keeps its sales by name`: after `dexDao.delete(id)`, the sale has `dexId == null` and `plantName == "Astro"`
  - `record refuses an invalid form`: quantity 0 → `null`
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*SaleRepositoryTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `SaleRepository` and wire up `ServiceLocator`.
- [ ] **Step 4: Run** the tests. Expected: PASS.
- [ ] **Step 5: Commit** `feat: record sales; selling the whole plant marks it sold and never deletes it`

### Task 4: The collection shows what's for sale and hides what's sold

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/ui/sales/SaleRibbon.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/collection/CollectionScreens.kt:118-119` (`owned` filter → `inCollection`; wishes stay `!owned`), `DexCard` (`:191`), and the chip row (`:120-123`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/breeding/BreedingViewModels.kt:41` (`it.owned` → `it.inCollection`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/dashboard/DashboardViewModel.kt:64-65` (`owned` → `inCollection` for the count and the cover)
- Test: `app/src/test/java/dev/zakyymuh/timebox/ui/dashboard/DashboardViewModelTest.kt`

**Interfaces:**
- Produces:
  - `@Composable fun SaleRibbon(price: Long?, modifier: Modifier = Modifier)`: a diagonal "SALE" band across the top-right corner in the dusty rose palette, plus the price in a white pill at the bottom (`Money.rupiah`, or no pill when the price is null). It's meant to sit inside a photo `Box` with `matchParentSize`.
  - `@Composable fun SaleBorder(): Modifier` (or a `Modifier.saleBorder()` extension): a 3.dp rose border with the card's corner radius.
- [ ] **Step 1: Write the failing test** `a sold plant is not counted or used as the cover` in `DashboardViewModelTest`: two owned plants, one with `soldOn` set → `owned == "1 plant"`, and the cover is never the sold plant's photo.
- [ ] **Step 2: Run** it. Expected: FAIL.
- [ ] **Step 3: Implement:**
  - switch the three filters to `inCollection`
  - in `DexCard`, when `entry.forSale`, add `SaleBorder` and overlay `SaleRibbon(entry.askingPrice)` on the photo
  - add a third chip, `For sale · n`, shown only when n > 0. It filters the collection page in place: a `var forSaleOnly` state, with no new pager page.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS. Then build and look at it on the phone (see memory: push, CI, `adb install -r`) with one plant marked for sale through a debug DB edit, or wait until after Task 5.
- [ ] **Step 5: Commit** `feat: for-sale plants wear a SALE ribbon and price in the collection; sold plants leave it`

### Task 5: Sell from a plant's page

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/ui/sales/SaleSheets.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/collection/CollectionViewModels.kt` (`DexDetailViewModel` takes `sales: SaleRepository`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/collection/CollectionScreens.kt` (`DexDetailScreen`: a "Sell" action next to Repot/Breed, only when `e.inCollection`; a "Sold on d MMM yyyy" pill instead of the actions when `e.soldOn != null`; the ribbon on the header photo when `e.forSale`)
- Modify: `AppNav.kt`, only where `DexDetailViewModel` is constructed. **This is a 9a-shared file: follow the Global Constraints check first.**
- Test: `app/src/test/java/dev/zakyymuh/timebox/ui/collection/DexDetailSellTest.kt` (a VM test with an in-memory DB)

**Interfaces:**
- Consumes: `SaleRepository` from Task 3, `SaleRibbon` from Task 4, and `Sales.suggestedPrice` and `SaleForm` from Task 1.
- Produces:
  - `@Composable fun SellSheet(forSale: Boolean, asking: Long?, onSave: (Boolean, Long?) -> Unit, onRecord: () -> Unit, onDismiss: () -> Unit)`: a *For sale* switch, a `PriceField` (make it `internal`), and a "Record a sale" button
  - `@Composable fun RecordSaleSheet(initial: SaleForm, asking: Long?, onSave: (SaleForm) -> Unit, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null)`: four kind chips, a quantity stepper (hidden for WHOLE), and a price that's prefilled with `suggestedPrice` until the owner edits it. It also has the date picker (reuse the one the edit screen uses for `acquired`), a note field, and Delete when `onDelete != null`. Task 6 reuses it for editing.
  - `DexDetailViewModel` gains `setForSale(forSale: Boolean, asking: Long?)` and `recordSale(form: SaleForm, onDone: (Boolean) -> Unit)`
- [ ] **Step 1: Write the failing tests:**
  - `recording a whole sale from the page marks the plant sold`: `state` emits an entry with `soldOn` set
  - `marking for sale shows on the page`: `forSale == true`, `askingPrice == 150_000`
- [ ] **Step 2: Run** them. Expected: FAIL.
- [ ] **Step 3: Implement** the sheets and the VM methods. A WHOLE sale is preselected when the plant is for sale, and PUP when it isn't.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS. Then install on the phone: mark a plant for sale (the ribbon shows in the grid), record a pup sale (it stays), then record a whole sale (it leaves the grid).
- [ ] **Step 5: Commit** `feat: sell from a plant's page: put it up for sale, record a pup, cutting, seedling or the whole plant`

### Task 6: The Sales page and its 💲 dashboard tile

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/ui/sales/SalesScreen.kt`
- Create: `app/src/main/java/dev/zakyymuh/timebox/ui/sales/SalesViewModel.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/Destinations.kt` (`const val SALES = "sales"`)
- Modify: `AppNav.kt` (the `composable(Destinations.SALES)` and `onOpenSales` for the dashboard)
- Modify: `DashboardViewModel.kt` (state `sales: String`) and `DashboardScreen.kt` (`SalesTile`)
- All three nav and dashboard files are 9a-shared: **run the Global Constraints check first.**
- Test: `app/src/test/java/dev/zakyymuh/timebox/ui/sales/SalesViewModelTest.kt` and `DashboardViewModelTest.kt`

**Interfaces:**
- Consumes: `SaleRepository` (`salesFlow`, `forSaleFlow`, `edit`, `delete`, `todayDate`), `Sales.totals` and `Sales.line`, `SaleRibbon`, `SaleBorder` and `RecordSaleSheet`.
- Produces:
  - `data class SalesState(val totals: SalesTotals, val forSale: List<DexEntry>, val sold: List<Sale>)`
  - `class SalesViewModel(repo: SaleRepository) : ViewModel()` with `val state: StateFlow<SalesState?>` (null while loading), `fun edit(id: Long, form: SaleForm)` and `fun delete(id: Long)`
  - `@Composable fun SalesScreen(viewModel: SalesViewModel, onBack: () -> Unit, onOpenPlant: (Long) -> Unit)`:
    - the top has `"{items} sold · Rp …"` and `"this month: {monthItems} · Rp …"`
    - two `Choice` chips over a `HorizontalPager`, *For sale · n* and *Sold · n*, the same as the collection screen
    - For sale is a 2-column grid of `DexCard`-style cards with `SaleBorder` and `SaleRibbon`; a tap opens the plant
    - Sold is a list of `Sales.line(sale)` with the date. A tap opens the plant when `dexId != null`. A long-press opens `RecordSaleSheet` with `onDelete`.
    - empty hints: "Nothing for sale. Open a plant and tap Sell." and "Nothing sold yet."
  - the dashboard state gains `sales: String`: `"Rp 420.000 this month"`, or `"Nothing sold yet"` when there are no sales at all, or `"Rp 0 this month"` when nothing sold this month
  - `SalesTile`: a square `Tile` with a 💲 icon circle, label `"Sales"`, title `state.sales`, description `"Open sales"`. It goes in a new `Row` under Collection/History, next to a `Spacer(Modifier.weight(1f))`. **This is my call; the owner can move it.**
- [ ] **Step 1: Write the failing tests:**
  - `SalesViewModelTest`: `state shows totals, for-sale plants and sales newest first`, and `deleting a sale updates the totals`
  - `DashboardViewModelTest`: `sales line is nothing sold yet with no sales` and `sales line shows this month's total`
- [ ] **Step 2: Run** them. Expected: FAIL.
- [ ] **Step 3: Implement** the screen, the VM, the route, and the tile. `DashboardViewModel` takes `SaleRepository` as well; update its construction in `AppNav` and the test's setup.
- [ ] **Step 4: Run** `./gradlew :core:test :app:testDebugUnitTest`. Expected: all PASS. Install on the phone and check the walk-through: dashboard 💲 → Sales → the totals match the recorded sales → long-press to delete the whole sale → the plant is back in the collection.
- [ ] **Step 5: Commit** `feat: a Sales page with totals, for-sale cards and every sale, behind a 💲 tile on the dashboard`
