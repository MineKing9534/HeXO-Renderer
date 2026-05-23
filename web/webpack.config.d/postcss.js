config.module.rules.push({
  test: /\.css$/i,
  use: [
    {
      loader: "postcss-loader",
      options: {
        postcssOptions: {
          plugins: [
            require("@tailwindcss/postcss"),
          ],
        },
      },
    },
  ],
})
