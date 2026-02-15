import '@testing-library/jest-dom'

class ResizeObserver {
	observe() {}
	unobserve() {}
	disconnect() {}
}

global.ResizeObserver = ResizeObserver

const localStorageStore = {}
global.localStorage = {
	getItem: (key) => (key in localStorageStore ? localStorageStore[key] : null),
	setItem: (key, value) => {
		localStorageStore[key] = String(value)
	},
	removeItem: (key) => {
		delete localStorageStore[key]
	},
	clear: () => {
		Object.keys(localStorageStore).forEach((key) => delete localStorageStore[key])
	}
}
