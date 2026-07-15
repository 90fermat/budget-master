// SQLDelight web worker support: sql.js must not resolve Node-only modules,
// and its wasm binary must be served next to the bundle.
config.resolve = {
    ...(config.resolve || {}),
    fallback: {
        ...((config.resolve || {}).fallback || {}),
        fs: false,
        path: false,
        crypto: false,
    },
};

const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm'
        ]
    })
);
