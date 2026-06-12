import {
  evaluatePassword,
  PASSWORD_REGEX,
  PASSWORD_RULES,
} from "@/lib/auth/passwordPolicy";

describe("passwordPolicy", () => {
  describe("evaluatePassword", () => {
    it("fails on an empty password", () => {
      const result = evaluatePassword("");
      expect(result.allPassed).toBe(false);
      // Min-length, character-class, and no-whitespace rules all fail.
      expect(result.rules.find((r) => r.id === "min-length")?.passed).toBe(
        false,
      );
      expect(
        result.rules.find((r) => r.id === "no-whitespace")?.passed,
      ).toBe(false);
    });

    it("fails on a short alphabetic-only password like 'abc'", () => {
      const result = evaluatePassword("abc");
      expect(result.allPassed).toBe(false);
      expect(result.rules.find((r) => r.id === "min-length")?.passed).toBe(
        false,
      );
      expect(result.rules.find((r) => r.id === "uppercase")?.passed).toBe(
        false,
      );
      expect(result.rules.find((r) => r.id === "digit")?.passed).toBe(false);
      expect(result.rules.find((r) => r.id === "special")?.passed).toBe(false);
    });

    it("passes for 'Abcdefghij1!' (10 chars, all classes)", () => {
      const result = evaluatePassword("Abcdefghij1!");
      expect(result.allPassed).toBe(true);
      expect(result.rules.every((r) => r.passed)).toBe(true);
    });

    it("fails when the password contains whitespace", () => {
      const result = evaluatePassword("Abcdefgh j1!");
      expect(result.allPassed).toBe(false);
      expect(
        result.rules.find((r) => r.id === "no-whitespace")?.passed,
      ).toBe(false);
    });

    it("fails the max-length rule on a 129-char password", () => {
      // Build a 129-char password that satisfies all OTHER rules so we are
      // sure max-length is the rule failing.
      const tail = "A".repeat(129 - "abcdefghij1!".length);
      const pw = "abcdefghij1!" + tail;
      expect(pw.length).toBe(129);
      const result = evaluatePassword(pw);
      expect(result.allPassed).toBe(false);
      expect(result.rules.find((r) => r.id === "max-length")?.passed).toBe(
        false,
      );
    });
  });

  describe("PASSWORD_REGEX", () => {
    it("matches a strong password", () => {
      expect(PASSWORD_REGEX.test("Abcdefghij1!")).toBe(true);
    });

    it("rejects whitespace", () => {
      expect(PASSWORD_REGEX.test("Abcdefgh j1!")).toBe(false);
    });

    it("rejects a password missing a digit", () => {
      expect(PASSWORD_REGEX.test("Abcdefghij!")).toBe(false);
    });
  });

  it("exports seven rules", () => {
    expect(PASSWORD_RULES).toHaveLength(7);
  });
});
