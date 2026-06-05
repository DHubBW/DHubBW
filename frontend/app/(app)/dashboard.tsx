import { Text, View } from 'react-native';

// TODO FE-Dashboard (#213): echter Dashboard-Screen mit News, Mensa-Preview, etc.
export default function DashboardScreen() {
  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: '600', color: '#000' }}>Dashboard</Text>
      <Text style={{ marginTop: 8, color: '#000' }}>TODO FE-Dashboard #213</Text>
    </View>
  );
}
