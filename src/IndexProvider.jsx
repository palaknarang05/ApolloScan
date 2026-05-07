import * as React from "react";

// Create a context
const IndexContext = React.createContext();

// Create a provider component
const IndexProvider = ({ children }) => {
  const [selectedIndex, setSelectedIndex] = React.useState(0);

  return (
    <IndexContext.Provider value={{ selectedIndex, setSelectedIndex }}>
      {children}
    </IndexContext.Provider>
  );
};

export { IndexProvider, IndexContext };