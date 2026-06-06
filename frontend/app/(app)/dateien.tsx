import { Text, View } from 'react-native';

/**
 * Files screen (placeholder).
 *
 * Will be filled with a file list, upload and folder tree by the FE-Dateien issue.
 * Only visible to users with a course membership.
 *
 * @returns The files screen.
 */
// TODO FE-Dateien: real files screen (list + upload + folder tree).
export default function DateienScreen() {
  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: '600', color: '#000' }}>Dateien</Text>
      <Text style={{ marginTop: 8, color: '#000' }}>TODO FE-Dateien</Text>
    </View>
  );
}
