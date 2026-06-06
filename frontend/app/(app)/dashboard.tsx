import { Text, View } from 'react-native';

/**
 * Dashboard screen (placeholder).
 *
 * Will be filled with real content by issue FE-Dashboard (#213)
 * (news feed, Mensa preview, course info, etc.).
 *
 * @returns The dashboard screen.
 */
// TODO FE-Dashboard (#213): real dashboard screen with news, Mensa preview, etc.
export default function DashboardScreen() {
  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: '600', color: '#000' }}>Dashboard</Text>
      <Text style={{ marginTop: 8, color: '#000' }}>TODO FE-Dashboard #213</Text>
    </View>
  );
}
