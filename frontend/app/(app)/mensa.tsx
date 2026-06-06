import { Text, View } from 'react-native';

/**
 * Mensa screen (placeholder).
 *
 * Will be filled with a real meal plan and week selector by the FE-Mensa issue.
 *
 * @returns The Mensa screen.
 */
// TODO FE-Mensa: real Mensa screen (meal plan, week selector).
export default function MensaScreen() {
  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: '600', color: '#000' }}>Mensa</Text>
      <Text style={{ marginTop: 8, color: '#000' }}>TODO FE-Mensa</Text>
    </View>
  );
}
