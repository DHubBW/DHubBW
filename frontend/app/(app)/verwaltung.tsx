import { Text, View } from 'react-native';

/**
 * Administration screen (placeholder).
 *
 * Will be filled with course and platform management features for KURSADMIN
 * and SYSTEMADMIN roles by the FE-Verwaltung issue.
 *
 * @returns The administration screen.
 */
// TODO FE-Verwaltung: real administration screen (KURSADMIN / SYSTEMADMIN).
export default function VerwaltungScreen() {
  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: '600', color: '#000' }}>Verwaltung</Text>
      <Text style={{ marginTop: 8, color: '#000' }}>TODO FE-Verwaltung</Text>
    </View>
  );
}
