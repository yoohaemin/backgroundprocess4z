import { defineUserConfig, defaultTheme } from 'vuepress'

export default defineUserConfig({
	base: '/',
	locales: {
		'/': {
			lang: 'en-US',
			title: 'Backgroundprocess4z',
			description: 'Background Process for ZIO',
		},
		//'/ko/': {
		//	lang: 'ko-KR',
		//}
	},
	theme: defaultTheme({
		repo: 'yoohaemin/backgroundprocess4z',
		locales: {
			'/': {
				navbar: [{
					text: 'Home',
					link: '/',
				},{
					text: 'Guide',
					link: '/guide/getting-started.html'
				}],
				selectLanguageName: 'English',
				sidebar: {
					'/guide/': [{
						text: 'Guide',
						children: [
							'/guide/getting-started.md',
							'/guide/defining-relations.md'
						],
					}],
					//'/reference/': [
					//  {
					//    text: 'Reference',
					//    children: ['/reference/cli.md', '/reference/config.md'],
					//  },
					//],
				},



			},
			//'/ko/': {
			//	selectLanguageName: '한국어',
			//},
		},
	}),
	markdown: {
		code: {
			lineNumbers: false
		}
	}
})
