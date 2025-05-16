import React, {
  createContext,
  useState,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from 'react'

type Value = Record<string, any> | undefined

// Create the context with default values
const ThemeContext = createContext<{
  user: Value
  setUser: Dispatch<SetStateAction<Value>>
}>({
  user: undefined,
  setUser: () => {},
})

export const UserProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<Value>()
  return (
    <ThemeContext.Provider value={{ user, setUser }}>
      {children}
    </ThemeContext.Provider>
  )
}

export const useUser = () => React.useContext(ThemeContext)
