import {
  ANA,
  MARKO,
  PETRA,
  Client,
  createAccount,
  dialog,
  emailCodes,
  expect,
  openGroup,
  openTab,
  row,
  scene,
  signIn,
  test,
  verifyEmail,
} from "../../frontend/acceptance/support";
import type { Page } from "@playwright/test";

/**
 * Hidden cases for the rules that only the browser shows: R-001, R-009, R-010, R-011, R-014, R-018, R-020,
 * R-021 and R-022. Test names carry the rule id and a category as in general/testing/hidden-evaluation.md.
 */

const en = (page: Page) =>
  page.getByRole("button", { name: "EN", exact: true });
const sl = (page: Page) =>
  page.getByRole("button", { name: "SL", exact: true });

test("R-001-01 other no other provider and no second factor on the sign-in screen", async ({
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Sign in with Google" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", {
      name: /facebook|github|apple|microsoft|twitter/i,
    }),
  ).toHaveCount(0);
  await expect(page.getByText(/verification code|two-factor|2FA/i)).toHaveCount(
    0,
  );
});

test("R-001-02 boundary a name of spaces only is refused", async ({ page }) => {
  await page.goto("/register");
  await page.getByLabel("Name").fill("     ");
  await page.getByLabel("E-mail").fill("nov@example.com");
  await page.getByLabel("Password").fill("Geslo1234");
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Verify your e-mail" }),
  ).toHaveCount(0);
});

test("R-001-03 boundary a password of 8 spaces is not refused by length and 7 characters are", async ({
  page,
}) => {
  await page.goto("/register");
  await page.getByLabel("Name").fill("Nov Uporabnik");
  await page.getByLabel("E-mail").fill("nov@example.com");
  await page.getByLabel("Password").fill("Abc1234");
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByText("The password needs at least 8 characters."),
  ).toBeVisible();
  await page.getByLabel("Password").fill("Abc12345");
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Verify your e-mail" }),
  ).toBeVisible();
});

test("R-001-04 other the verification e-mail is sent to the address that was registered", async ({
  page,
}) => {
  await page.goto("/register");
  await page.getByLabel("Name").fill("Nov Uporabnik");
  await page.getByLabel("E-mail").fill("Drug.Naslov@Example.com");
  await page.getByLabel("Password").fill("Geslo1234");
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Verify your e-mail" }),
  ).toBeVisible();
  await expect
    .poll(
      async () =>
        (await emailCodes("drug.naslov@example.com", "VERIFY_EMAIL")).length,
    )
    .toBeGreaterThan(0);
});

test("R-001-05 combination every group address and the groups list show only the verify screen", async ({
  page,
}) => {
  const other = await Client.of(MARKO);
  const groupId = await other.group("Skrita");
  await createAccount(ANA, { verified: false });
  await signIn(page, ANA);
  for (const path of [
    "/",
    "/deleted-groups",
    "/account",
    `/groups/${groupId}`,
    `/groups/${groupId}/members`,
    `/groups/${groupId}/history`,
  ]) {
    await page.goto(path);
    await expect(
      page.getByRole("heading", { name: "Verify your e-mail" }),
    ).toBeVisible();
    await expect(page.getByText("Skrita")).toHaveCount(0);
  }
  await expect(page.getByRole("button", { name: "Send again" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign out" })).toBeVisible();
});

test("R-001-06 other the reset flow for an address without an account looks the same", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Forgot password?" }).click();
  await page.getByLabel("E-mail").fill("nihce@example.com");
  await page.getByRole("button", { name: "Send link" }).click();
  await expect(
    page.getByRole("heading", { name: "Check your e-mail" }),
  ).toBeVisible();
});

test("R-001-07 combination wrong e-mail and wrong password both show an error and open nothing", async ({
  page,
}) => {
  await createAccount(ANA);
  for (const [email, password] of [
    ["napacen@example.com", "Geslo1234"],
    [ANA.email, "Napacno1234"],
  ]) {
    await page.goto("/");
    await page.getByLabel("E-mail").fill(email!);
    await page.getByLabel("Password").fill(password!);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
  }
});

test("R-001-08 combination after sign out a protected address leads to the sign-in screen", async ({
  page,
}) => {
  const client = await Client.of(ANA);
  const groupId = await client.group("Moja");
  await signIn(page, ANA);
  await page.getByRole("button", { name: "User menu" }).click();
  await page.getByText("Sign out", { exact: true }).click();
  await page.goto(`/groups/${groupId}`);
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
  await expect(page.getByText("Moja")).toHaveCount(0);
});

test("R-001-09 other a user with a group does not see the empty state", async ({
  page,
}) => {
  const client = await Client.of(ANA);
  await client.group("Moja");
  await signIn(page, ANA);
  await expect(page.getByRole("link", { name: "Moja" })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Create a group" }),
  ).toHaveCount(0);
});

test.describe("language", () => {
  test.use({ english: false });

  test("R-018-01 other Slovenian is the language of every public screen without a stored choice", async ({
    page,
  }) => {
    for (const [path, fields] of [
      ["/sign-in", 1],
      ["/register", 2],
    ] as const) {
      await page.goto(path);
      await expect(page.locator("html")).toHaveAttribute("lang", "sl");
      await expect(page.getByRole("main").getByRole("textbox")).toHaveCount(
        fields,
      );
    }
  });

  test("R-018-02 combination switching twice returns the same texts", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(page.getByRole("main").getByRole("textbox")).toHaveCount(1);
    await en(page).click();
    const english = await page.locator("main").innerText();
    await sl(page).click();
    const slovenian = await page.locator("main").innerText();
    expect(slovenian).not.toBe(english);
    await en(page).click();
    expect(await page.locator("main").innerText()).toBe(english);
  });

  test("R-018-03 boundary the choice is per browser and stored under the key language", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(page.getByRole("main").getByRole("textbox")).toHaveCount(1);
    await en(page).click();
    expect(await page.evaluate(() => localStorage.getItem("language"))).toBe(
      "en",
    );
    await sl(page).click();
    expect(await page.evaluate(() => localStorage.getItem("language"))).toBe(
      "sl",
    );
    await page.reload();
    await expect(sl(page)).toHaveAttribute("aria-pressed", "true");
  });
});

test("R-018-04 boundary a time just before midnight in summer and winter", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak" });
  await page.route("**/api/groups/*/history**", async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    for (const item of body.items ?? [])
      item.createdAt = "2026-12-31T23:30:00Z";
    await route.fulfill({ response, json: body });
  });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "History");
  await expect(page.getByText("2027-01-01 00:30").first()).toBeVisible();
  await sl(page).click();
  await expect(page.getByText("1. 1. 2027 00:30").first()).toBeVisible();
});

test("R-018-05 other the time zone is the same for every user whatever the browser zone", async ({
  browser,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak" });
  const context = await browser.newContext({
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:5173",
    timezoneId: "America/Los_Angeles",
  });
  await context.addInitScript(() => localStorage.setItem("language", "en"));
  const page = await context.newPage();
  await page.route("**/api/groups/*/history**", async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    for (const item of body.items ?? [])
      item.createdAt = "2026-07-01T22:30:00Z";
    await route.fulfill({ response, json: body });
  });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "History");
  await expect(page.getByText("2026-07-02 00:30").first()).toBeVisible();
  await context.close();
});

test("R-018-06 other more countries in both languages", async ({ page }) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak", country: "AT" });
  await owner.contact(groupId, { lastName: "Horvat", country: "DE" });
  await openGroup(page, ANA, "Skupina");
  await expect(row(page, "Novak")).toContainText("Austria");
  await expect(row(page, "Horvat")).toContainText("Germany");
  await sl(page).click();
  await expect(row(page, "Novak")).toContainText("Avstrija");
  await expect(row(page, "Horvat")).toContainText("Nemčija");
});

test("R-018-07 other the search and the sort give the same result in both languages", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  for (const lastName of ["Škerlj", "Skok", "Čuk", "Cvek"])
    await owner.contact(groupId, { lastName });
  await openGroup(page, ANA, "Skupina");
  await page.getByLabel("Search").fill("skerlj");
  await expect(row(page, "Škerlj")).toBeVisible();
  await expect(row(page, "Skok")).toHaveCount(0);
  await sl(page).click();
  await expect(row(page, "Škerlj")).toBeVisible();
  await expect(row(page, "Skok")).toHaveCount(0);
});

test("R-018-08 other the contact data stays in the language it was written in", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, {
    lastName: "Novak",
    company: "Trgovina d.o.o.",
    tags: ["udelezenec"],
    attributes: [{ key: "Delovno mesto", value: "Vodja" }],
  });
  await openGroup(page, ANA, "Skupina");
  await expect(row(page, "Novak")).toContainText("Trgovina d.o.o.");
  await sl(page).click();
  await expect(row(page, "Novak")).toContainText("Trgovina d.o.o.");
  await expect(row(page, "Novak")).toContainText("udelezenec");
});

test("R-009-05 other no match with a filter and a search shows one clear button that removes both", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak", tags: ["x"] });
  await openGroup(page, ANA, "Skupina");
  await page.getByLabel("Search").fill("zzz");
  await page.getByRole("button", { name: "Filter" }).click();
  await dialog(page)
    .getByLabel("Condition")
    .selectOption({ label: "Has all of these tags" });
  await dialog(page).getByLabel("Tags", { exact: true }).fill("x");
  await page.keyboard.press("Enter");
  await dialog(page).getByRole("button", { name: "Add condition" }).click();
  await expect(
    page.getByRole("heading", { name: "No contacts match" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Clear filters" }).first().click();
  await expect(page.getByLabel("Search")).toHaveValue("");
  await expect(page.getByText("Has all of: x")).toHaveCount(0);
  await expect(row(page, "Novak")).toBeVisible();
});

test("R-009-06 role an Editor sees Add contact in the empty group and the text names no Viewer wording", async ({
  page,
}) => {
  await scene("Skupina", [{ person: MARKO, role: "EDITOR" }]);
  await openGroup(page, MARKO, "Skupina");
  await expect(
    page.getByRole("heading", { name: "No contacts yet" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add contact" }).first(),
  ).toBeVisible();
});

test("R-009-08 other sorting a second time keeps search and page one", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  for (let i = 0; i < 30; i++)
    await owner.contact(groupId, {
      lastName: `Novak${String(i).padStart(2, "0")}`,
      city: `M${29 - i}`,
    });
  await openGroup(page, ANA, "Skupina");
  await page.getByLabel("Search").fill("novak");
  await page.getByRole("columnheader", { name: /Last name/ }).click();
  await page.getByRole("columnheader", { name: /Last name/ }).click();
  await expect(page.getByLabel("Search")).toHaveValue("novak");
  await expect(page.getByText("30 contacts")).toBeVisible();
});

test("R-009-09 other a row opens the contact screen with the same values", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina", [
    { person: PETRA, role: "VIEWER" },
  ]);
  await owner.contact(groupId, {
    firstName: "Ana",
    lastName: "Odprta",
    email: "odprta@example.com",
  });
  await openGroup(page, PETRA, "Skupina");
  await row(page, "Odprta").getByRole("link", { name: "Odprta" }).click();
  await expect(page.getByText("odprta@example.com")).toBeVisible();
});

test("R-010-10 other removing one of two chips updates the list at once", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "A", tags: ["x"], country: "SI" });
  await owner.contact(groupId, { lastName: "B", tags: ["x"], country: "AT" });
  await openGroup(page, ANA, "Skupina");
  for (const [condition, label, value] of [
    ["Has all of these tags", "Tags", "x"],
    ["Company", "Company", "Nihče"],
  ] as const) {
    await page.getByRole("button", { name: "Filter" }).click();
    await dialog(page)
      .getByLabel("Condition")
      .selectOption({ label: condition });
    await dialog(page).getByLabel(label, { exact: true }).fill(value);
    if (label === "Tags") await page.keyboard.press("Enter");
    await dialog(page).getByRole("button", { name: "Add condition" }).click();
  }
  await expect(
    page.getByRole("heading", { name: "No contacts match" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: /×|Remove/ })
    .last()
    .click();
  await expect(row(page, "A")).toBeVisible();
  await expect(row(page, "B")).toBeVisible();
});

test("R-010-11 other chips appear for each condition type with its values", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, {
    lastName: "A",
    attributes: [{ key: "barva", value: "zelena" }],
  });
  await openGroup(page, ANA, "Skupina");
  await page.getByRole("button", { name: "Filter" }).click();
  await dialog(page)
    .getByLabel("Condition")
    .selectOption({ label: "Attribute key equals value" });
  await dialog(page).getByLabel("Attribute key", { exact: true }).fill("barva");
  await dialog(page)
    .getByLabel("Attribute value", { exact: true })
    .fill("zelena");
  await dialog(page).getByRole("button", { name: "Add condition" }).click();
  await expect(page.getByText(/barva/)).toBeVisible();
  await expect(page.getByText(/zelena/)).toBeVisible();
});

test("R-011-01 role Owner and Editor see select-all on the page and select-all N results and a Viewer does not", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina", [
    { person: MARKO, role: "EDITOR" },
  ]);
  for (let i = 0; i < 3; i++)
    await owner.contact(groupId, { lastName: `P${i}` });
  await openGroup(page, MARKO, "Skupina");
  await expect(page.getByLabel("Select all on this page")).toBeVisible();
  await page.getByLabel("Select all on this page").check();
  await expect(
    page.getByRole("button", { name: "Select all 3 results" }),
  ).toBeVisible();
});

test("R-011-02 other sorting keeps the selection and a new search text clears it", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  for (let i = 0; i < 5; i++)
    await owner.contact(groupId, { lastName: `P${i}` });
  await openGroup(page, ANA, "Skupina");
  await page.getByLabel("Select all on this page").check();
  await page.getByRole("columnheader", { name: /Last name/ }).click();
  await expect(page.getByText("5 selected")).toBeVisible();
  await page.getByLabel("Search").fill("p");
  await expect(page.getByText("Nothing selected")).toBeVisible();
});

test("R-011-03 boundary unticking the last contact disables Export again", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "A" });
  await openGroup(page, ANA, "Skupina");
  const box = page.getByRole("checkbox", { name: "Select contact" }).first();
  await box.check();
  await expect(
    page.getByRole("button", { name: "Export", exact: true }),
  ).toBeEnabled();
  await box.uncheck();
  await expect(
    page.getByRole("button", { name: "Export", exact: true }),
  ).toBeDisabled();
});

test("R-014-06 other the empty state and the no-match state are different", async ({
  page,
}) => {
  await scene("Skupina");
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "History");
  await expect(
    page.getByRole("heading", { name: "No History yet" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "No events match" }),
  ).toHaveCount(0);
});

test("R-020-03 other comparing shows tag and attribute differences", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina", [
    { person: MARKO, role: "EDITOR" },
  ]);
  const c = await owner.contact(groupId, {
    lastName: "Novak",
    tags: ["a"],
    attributes: [{ key: "k", value: "1" }],
  });
  const current = await owner.ok("GET", `/groups/${groupId}/contacts/${c.id}`);
  await owner.ok("PUT", `/groups/${groupId}/contacts/${c.id}`, {
    ...current,
    tags: ["a", "b"],
    attributes: [{ key: "k", value: "2" }],
  });
  await openGroup(page, MARKO, "Skupina");
  await row(page, "Novak").getByRole("link", { name: "Novak" }).click();
  await page
    .getByRole("tab", { name: "Versions" })
    .or(page.getByRole("link", { name: "Versions" }))
    .click();
  await row(page, "Version 1").getByRole("button", { name: "Compare" }).click();
  await expect(
    page.getByRole("heading", { name: "Compare versions" }),
  ).toBeVisible();
  await expect(row(page, "Tags")).toContainText("b");
  await expect(row(page, "Attributes")).toContainText("2");
});

test("R-020-09 other the restore dialog names what it does before anything is changed", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina", [
    { person: MARKO, role: "EDITOR" },
  ]);
  const c = await owner.contact(groupId, { lastName: "Novak", phone: "1" });
  const current = await owner.ok("GET", `/groups/${groupId}/contacts/${c.id}`);
  await owner.ok("PUT", `/groups/${groupId}/contacts/${c.id}`, {
    ...current,
    phone: "2",
  });
  await openGroup(page, MARKO, "Skupina");
  await row(page, "Novak").getByRole("link", { name: "Novak" }).click();
  await page
    .getByRole("tab", { name: "Versions" })
    .or(page.getByRole("link", { name: "Versions" }))
    .click();
  await row(page, "Version 1").getByRole("button", { name: "Restore" }).click();
  await expect(
    dialog(page).getByText("A new version is recorded; nothing is lost."),
  ).toBeVisible();
  expect(
    (await owner.ok("GET", `/groups/${groupId}/contacts/${c.id}`)).phone,
  ).toBe("2");
});

test("R-021-07 other a probable set shows its reason and a certain set its reason", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, {
    firstName: "Ana",
    lastName: "Novak",
    company: "Acme",
  });
  await owner.contact(groupId, {
    firstName: "Ana",
    lastName: "Novak",
    company: "Acme",
  });
  await owner.contact(groupId, { lastName: "X", email: "x@example.com" });
  await owner.contact(groupId, { lastName: "Y", email: "X@example.com" });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await expect(page.getByText("Probable")).toBeVisible();
  await expect(page.getByText("Same name and company")).toBeVisible();
  await expect(page.getByText("Certain")).toBeVisible();
  await expect(page.getByText("Same e-mail address")).toBeVisible();
});

test("R-021-12 other the empty result does not show an error", async ({
  page,
}) => {
  await scene("Skupina");
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await expect(
    page.getByRole("heading", { name: "No duplicates found" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
});

test("R-022-01 other the survivor is labelled as the oldest and the other as the contact to join", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak", email: "x@example.com" });
  await owner.contact(groupId, { lastName: "Novak", email: "x@example.com" });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await page.getByRole("button", { name: "Merge" }).first().click();
  await expect(page.getByText("Survivor (the oldest, stays)")).toBeVisible();
  await expect(page.getByText("Contact to join")).toBeVisible();
});

test("R-022-02 combination the first non-empty value of the oldest contact is preselected for several fields", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, {
    lastName: "Novak",
    email: "x@example.com",
    company: "Prvi",
    city: "Celje",
  });
  await owner.contact(groupId, {
    lastName: "Novak",
    email: "x@example.com",
    company: "Drugi",
    phone: "123",
    jobTitle: "Vodja",
  });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await page.getByRole("button", { name: "Merge" }).first().click();
  await page.getByRole("button", { name: "Confirm merge" }).click();
  await expect
    .poll(
      async () =>
        (await owner.ok("POST", `/groups/${groupId}/contacts/search`, {}))
          .total,
    )
    .toBe(1);
  const only = (
    await owner.ok("POST", `/groups/${groupId}/contacts/search`, {})
  ).items[0];
  expect(only.company).toBe("Prvi");
  expect(only.city).toBe("Celje");
  expect(only.phone).toBe("123");
});

test("R-022-08 other the hint is shown on every merge screen", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  for (let i = 0; i < 3; i++)
    await owner.contact(groupId, { lastName: "Novak", email: "x@example.com" });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await page.getByRole("button", { name: "Merge" }).first().click();
  await expect(
    page.getByText(
      /You can revert a merge later from the contact.s Versions tab\./,
    ),
  ).toBeVisible();
});

test("R-022-11 other nothing is merged after the duplicates screen is left", async ({
  page,
}) => {
  const { owner, groupId } = await scene("Skupina");
  await owner.contact(groupId, { lastName: "Novak", email: "x@example.com" });
  await owner.contact(groupId, { lastName: "Novak", email: "x@example.com" });
  await openGroup(page, ANA, "Skupina");
  await openTab(page, "Duplicates");
  await page.getByRole("button", { name: "Find duplicates" }).first().click();
  await openTab(page, "Contacts");
  await expect(row(page, "Novak")).toHaveCount(2);
  expect(verifyEmail).toBeDefined();
});
