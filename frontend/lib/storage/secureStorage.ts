/**
 * Plattform-Abstraktion für Token-Persistenz.
 *
 * - **Web:** alle Methoden sind no-op. Das HttpOnly-`jwt`-Cookie ist Source of
 *   Truth; der Browser sendet es automatisch via `credentials: 'include'`.
 *   JS hat (per Design) keinen Zugriff darauf — dürfen wir auch gar nicht haben.
 *
 * - **Native (iOS/Android):** `expo-secure-store` (Keychain auf iOS,
 *   Keystore/EncryptedSharedPreferences auf Android). Wir speichern `token`
 *   und `expiresAt` getrennt. `getToken()` macht eine **proaktive
 *   Ablauf-Vorprüfung** — abgelaufene Token werden gelöscht und `null`
 *   zurückgegeben, damit fetchClient gar nicht erst einen Bearer-Header anhängt.
 *
 * Frozen Public API für Sub-B (siehe Sub-A-Handover Anhang).
 */
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

export interface StoredToken {
  token: string;
  expiresAt: number; // ms-since-epoch
}

export interface SecureStorage {
  setToken(v: StoredToken): Promise<void>;
  getToken(): Promise<string | null>;
  clear(): Promise<void>;
  getExpiresAt(): Promise<number | null>;
}

const TOKEN_KEY = 'dhubbw.jwt';
const EXPIRES_KEY = 'dhubbw.jwt_exp';

const webStorage: SecureStorage = {
  async setToken(_v: StoredToken): Promise<void> {
    // no-op auf Web — HttpOnly-Cookie übernimmt
  },
  async getToken(): Promise<string | null> {
    return null;
  },
  async clear(): Promise<void> {
    // no-op auf Web
  },
  async getExpiresAt(): Promise<number | null> {
    return null;
  },
};

const nativeStorage: SecureStorage = {
  async setToken(v: StoredToken): Promise<void> {
    await SecureStore.setItemAsync(TOKEN_KEY, v.token);
    await SecureStore.setItemAsync(EXPIRES_KEY, String(v.expiresAt));
  },

  async getToken(): Promise<string | null> {
    const token = await SecureStore.getItemAsync(TOKEN_KEY);
    if (!token) return null;
    const expRaw = await SecureStore.getItemAsync(EXPIRES_KEY);
    const exp = expRaw ? Number(expRaw) : NaN;
    if (!Number.isFinite(exp) || exp < Date.now()) {
      // Proaktiv aufräumen — fetchClient soll keinen abgelaufenen Bearer senden.
      await nativeStorage.clear();
      return null;
    }
    return token;
  },

  async clear(): Promise<void> {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    await SecureStore.deleteItemAsync(EXPIRES_KEY);
  },

  async getExpiresAt(): Promise<number | null> {
    const expRaw = await SecureStore.getItemAsync(EXPIRES_KEY);
    if (!expRaw) return null;
    const exp = Number(expRaw);
    return Number.isFinite(exp) ? exp : null;
  },
};

const secureStorage: SecureStorage = Platform.OS === 'web' ? webStorage : nativeStorage;

export default secureStorage;
