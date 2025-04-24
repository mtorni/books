import { Slot } from 'expo-router';
import { TailwindProvider } from 'tailwindcss-react-native';
import { View, Text } from 'react-native';
import "../global.css"

export default function Layout() {
  return (
    <TailwindProvider>
      <Slot />
      <View className="flex-1 justify-center items-center bg-blue-500">
        <Text className="text-white text-2xl">Tailwind is working!</Text>
      </View>
    </TailwindProvider>
  );
}
