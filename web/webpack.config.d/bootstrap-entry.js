const fs = require("fs")
const path = require("path")

const bootstrap = path.resolve(__dirname, "web-bootstrap.js")

fs.writeFileSync(bootstrap, /*language=js*/ `

import('./kotlin/HeXO-web.js').catch((error) => {
  console.error("Failed to load HeXO web app", error)

  const root = document.getElementById("root")
  if (root) {
    root.innerHTML = 
        '<div class="min-h-dvh w-full box-border grid place-items-center p-12 bg-slate-950 text-lg font-sans">' +
            '<textarea readonly class="box-border min-h-32 max-h-[calc(100dvh-2rem)] w-full max-w-2xl resize-y overflow-auto whitespace-pre-wrap rounded-lg border-3 border-rose-400 bg-slate-950 p-3 font-mono text-sm leading-relaxed text-rose-100 outline-none transition">' +
            'Failed to load the editor. Refresh the page to try again.' +
            '</textarea>' +
        '</div>'
  }
})
    
`)

config.entry = {
  main: bootstrap,
}
