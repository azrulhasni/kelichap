/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import { html, LitElement } from 'lit';
import { registerStyles, css } from '@vaadin/vaadin-themable-mixin/register-styles.js';
import { map } from 'lit/directives/map.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';
import { PolylitMixin } from '@vaadin/component-base/src/polylit-mixin.js';
import { ElementMixin } from "@vaadin/component-base";
import { ResizeController } from '@lit-labs/observers/resize-controller.js'
import '@vaadin/text-field';
import '@vaadin/list-box';
import '@vaadin/item';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import './vcf-autocomplete-overlay';

/**
 * `<vcf-autocomplete>` Web Component with a text input that provides a panel of suggested options.
 *
 * ```html
 * <vcf-autocomplete></vcf-autocomplete>
 * ```
 *
 * @memberof Vaadin
 * @mixes ElementMixin
 * @mixes ThemableMixin
 * @demo demo/index.html
 */

registerStyles(
        'vaadin-item',
        css`
        :host(.vcf-autocomplete-no-checkmark) [part="checkmark"] {
            visibility: hidden;
        }
    `
        );
class VcfAutocomplete extends ElementMixin(ThemableMixin(PolylitMixin(LitElement))) {

    static get styles() {
        return css`
            :host {
                display: inline-block;
            }

            :host([opened]) {
                pointer-events: auto;
            }
        `
    }

    render() {
        return html`
            <vaadin-text-area part="textArea" id="textArea" @focus="${this._textAreaFocused}" .label="${this.label}" .placeholder="${this.placeholder}" theme="${this._theme}" value="${this.value}" ?readonly="${this.readonly}" ?disabled="${!this.enabled}" style="width: 100%;">
                ${this._hasValue(this.value) && !this.readonly && this.enabled ? html`
                    <vaadin-button part="clear" theme="icon tertiary small" aria-label="Add new item" slot="suffix" @click="${this.clear}">
                        <vaadin-icon icon="lumo:cross"> </vaadin-icon>
                    </vaadin-button>
                ` : ''}
            </vaadin-text-area>

            <vcf-autocomplete-overlay .opened="${this.opened}" theme="${this._theme}">
                <vaadin-list-box part="options-container" theme="${this._theme}">
                    ${!this.loading ? html`
                        ${map(this._limitedOptions, (option) => html`
                            <vaadin-item @click="${this._optionClicked}" part="option" theme="${this._theme}" class="vcf-autocomplete-no-checkmark">
                                ${this._getSuggestedStart(this.value, option)} <span part="bold" style="font-weight: 600;">${this._getInputtedPart(this.value, option)}</span>${this._getSuggestedEnd(this.value, option)}
                            </vaadin-item>
                        `)}
                    ` : ''}

                    ${this._noResultsShown(this.options, this.loading) ? html`
                        <vaadin-item disabled part="option">
                            <div part="no-results">No results</div>
                        </vaadin-item>
                    ` : ''}

                    ${this.loading ? html`
                        <vaadin-item part="loading" disabled part="option">
                            <div part="loading-indicator">Loading...</div>
                        </vaadin-item>
                    ` : ''}
                </vaadin-list-box>
            </vcf-autocomplete-overlay>
        `;
    }

    static get is() {
        return 'vcf-autocomplete';
    }

    static get version() {
        return '1.2.10';
    }

    static get properties() {
        return {
            opened: {
                type: Boolean,
                reflectToAttribute: true,
                value: false,
                observer: '_openedChange'
            },

            value: {
                type: String,
                notify: true,
                observer: '_valueChange'
            },

            options: {
                type: Array,
                value: () => [],
                observer: '_optionsChange'
            },

            limit: {
                type: Number,
                value: 10
            },

            loading: {
                type: Boolean,
                reflectToAttribute: true,
                value: false
            },

            label: {
                type: String
            },
            
            openerChar: {
                type: String
            },

            placeholder: {
                type: String
            },

            caseSensitive: {
                type: Boolean,
                value: false
            },

            _limitedOptions: {
                type: Array
            },

            _savedValue: {
                type: String
            },

            _keepClosedAfterNextValueChange: {
                type: Boolean,
                value: false
            },

            readonly: {
                type: Boolean,
                value: false,
                reflectToAttribute: true
            },

            enabled: {
                type: Boolean,
                value: true,
                reflectToAttribute: true
            },

            _overlayElement: Object,
            _optionsContainer: Object,
            _selectedOption: {
                type: Object,
                value: undefined,
                observer: '_selectedOptionChanged'
            },
            _boundOutsideClickHandler: Object,
            _boundSetOverlayPosition: Object,
            _currentMentionCharIndex: Number,
            _cursorIndex: Number
        };
    }

    static _finalizeClass() {
        super._finalizeClass();
    }

    constructor() {
        super();
        this._boundSetOverlayPosition = this._setOverlayPosition.bind(this);
        this._boundOutsideClickHandler = this._outsideClickHandler.bind(this);
        this._resizeController = new ResizeController(this, {
            target: this,
            callback: this._boundSetOverlayPosition,
            skipInitial: true
        });
        this._currentMentionCharIndex = 0;
        this._cursorIndex = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        document.addEventListener('click', this._boundOutsideClickHandler);
    }

    firstUpdated() {
        super.ready();

        this._textArea = this.shadowRoot.getElementById('textArea');
        this._overlayElement = this.shadowRoot.querySelector('vcf-autocomplete-overlay');
        this._optionsContainer = this._overlayElement.querySelector('vaadin-list-box');

        this._textArea.addEventListener('input', this._onInput.bind(this));
        this.addEventListener('click', this._elementClickListener);
        this.addEventListener('keydown', this._onKeyDown.bind(this));
        this.addEventListener('keyup', this._onKeyUp.bind(this));
        this._overlayElement.addEventListener('vaadin-overlay-outside-click', ev => ev.preventDefault());
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        document.removeEventListener('click', this._boundOutsideClickHandler);
    }

    _elementClickListener(event) {
        event.stopPropagation();
    }

    _setOverlayPosition() {
        const inputRect = this._textArea.getBoundingClientRect();

        this._overlayElement.style.left = inputRect.left + 'px';
        this._overlayElement.style.top = inputRect.bottom + window.pageYOffset + 'px';

        this._overlayElement.updateStyles({'--vcf-autocomplete-options-width': inputRect.width + 'px'});
    }

    _outsideClickHandler() {
        this.opened = false;
    }

    _valueChange(value) {

        if (this._selectedOption) {
            this._selectedOption._setFocused(false);
            this._selectedOption = null;
        }

//        if (value.length > 0 && !this.opened && !this._keepClosedAfterNextValueChange) {
//           this.opened = true;
//        } else if (value.length == 0 && this.opened) {
//            this.opened = false;
//        }
//        this._keepClosedAfterNextValueChange = false;
//        this.__previousValue = value;
    }

//    _optionClicked(ev) {
//        this._savedValue = this.value;
//        this._applyValue(ev.srcElement.value);
//    }

    _applyValue(value) {
        this.dispatchEvent(
                new CustomEvent('vcf-autocomplete-value-applied', {
                    bubbles: true,
                    detail: {
                        value: value
                    }
                })
                );

        this._setValue(value);
        //this._setValue(this._savedValue.substring(0, this._currentMentionCharIndex+1)+value+" "+this._savedValue.substring(this._currentMentionCharIndex+1));
        this.opened = false;
        //this._textArea.blur();
    }

    _setValue(value) {
        //this._keepClosedAfterNextValueChange = true;
        this.value = value;
    }

    _textAreaFocused() {
        if (this.value && this.value.length > 0) {
            this.opened = true;
        }
    }

    _hasValue(value) {
        return typeof (value) !== 'undefined' && value.length > 0;
    }

    _optionsChange(options) {
        this._limitedOptions = options.slice(0, this.limit);
    }

    clear() {
        this._setValue('');
        this._emitInputEvent()
        this._applyValue('')
        this._selectedOptionChanged(null)
        this.dispatchEvent(
                new Event('clear', {
                    bubbles: true,
                    cancelable: true
                })
                );
        this._textArea.focus();
    }

    _getSuggestedStart(value, option) {
        if (!value) {
            return;
        }
        return option.substr(0, this._getValueIndex(value, option));
    }

    _getInputtedPart(value, option) {
        if (!value) {
            return option;
        }
        return option.substr(this._getValueIndex(value, option), value.length);
    }

    _getSuggestedEnd(value, option) {
        if (!value) {
            return;
        }
        return option.substr(this._getValueIndex(value, option) + value.length, option.length);
    }

    _getValueIndex(value, option) {
        if (!this.caseSensitive) {
            value = value.toLowerCase();
            option = option.toLowerCase();
        }
        return option.indexOf(value) >= 0 ? option.indexOf(value) : 0;
    }

    _onKeyUp(event) {
        if (/[\p{L}\p{N}]/gu.test(String.fromCharCode(event.keyCode))) { //match letters and numbers unicode character
            let currentCursor = event.target._textArea.lastChild.selectionStart;
            let token = this._textArea.value.toString().substring(this._currentMentionCharIndex + 1, currentCursor + 1);

            this.dispatchEvent(
                    new CustomEvent('vcf-autocomplete-value-token', {
                        bubbles: true,
                        detail: {
                            token: token
                        }
                    })
                    );
        }
    }

    _onKeyDown(event) {
        //console.log('opener:'+this.openerChar);
        if (event.key === this.openerChar){//'@') {
            this.opened = true;
            this._currentMentionCharIndex = event.target._textArea.lastChild.selectionStart;

        }
        this._cursorIndex = event.target._textArea.lastChild.selectionStart;

        const key = event.key.replace(/^Arrow/, '');
        switch (key) {
            case 'Down':
                event.preventDefault();
                this._navigate('next');

                break;
            case 'Up':
                event.preventDefault();
                this._navigate('prev');

                break;
            case 'Enter':
                if (this.opened) {
                    event.preventDefault(); //prevent return carriage in the textarea
                }
                if (this._selectedOption) {
                    let selected = this._selectedOption.value;
                    if (this._selectedOption.value.substring(0, 1)=== this.openerChar){ //if the first char of the selected option is the opener char, ignore it
                        selected = this._selectedOption.value.substring(1);
                    }
                    
                    
                    if (this._savedValue.charAt(this._currentMentionCharIndex) === this.openerChar) {
                        let newvalue = this._savedValue.substring(0, this._currentMentionCharIndex + 1) + selected + " " + this._savedValue.substring(this._cursorIndex);
                        this._applyValue(newvalue);
                    } else {
                        let newvalue = this._savedValue.substring(0, this._currentMentionCharIndex + 1) + this.openerChar + selected + " " + this._savedValue.substring(this._cursorIndex);
                        this._applyValue(newvalue);
                    }
                } else {
                    this._applyValue(this._textArea.value);
                }
                break;
            case 'Escape':
                this.opened = false;

                break;
        }


    }

    _navigate(to) {
        const items = this._optionsContainer.items.filter(item => !item.disabled);

        if (!items.length) {
            return;
        }

        const index = items.indexOf(this._selectedOption);

        // Store the current value if an arrow clicked in the first time
        if (index === -1) {
            this._savedValue = this.value;
        }

        // Reset the previously selected option
        if (this._selectedOption) {
            this._selectedOption._setFocused(false);
            this._selectedOption = null;
        }

        let nextIndex;

        // Calculate where to navigate next
        if (to === 'next') {
            nextIndex = index + 1;

            // If out of bounds then navigate to -1, which means 'previously stored value'
            if (nextIndex > items.length - 1) {
                nextIndex = -1;
            }
        } else if (to === 'prev') {
            nextIndex = index - 1;

            // If out of bounds then navigate to -1, which means 'previously stored value'
            if (nextIndex < -1) {
                nextIndex = items.length - 1;
            }
        }

        // Navigate to the next option
        if (nextIndex >= 0) {
            items[nextIndex]._setFocused(true);
            this._selectedOption = items[nextIndex];

            return this._selectedOption.value;
            // or restore the saved value
        } else {
            this._textArea.value = this._savedValue;

            return this._savedValue;
        }
    }

    _selectedOptionChanged(selectedOption) {
        if (typeof selectedOption === 'undefined' || !selectedOption) {
            return;
        }
        this._selectedValue = selectedOption.value;
    }

    _emitInputEvent() {
        this._textArea.dispatchEvent(
                new Event('input', {
                    bubbles: true,
                    cancelable: true
                })
                );
    }

    _onInput(event) {
        this.value = event.target.value;
    }

    _noResultsShown(options, loading) {
        return (!options || !options.length) && !loading;
    }

    _openedChange(opened) {
        if (opened) {
            this._setOverlayPosition();
        }
    }
}

customElements.define(VcfAutocomplete.is, VcfAutocomplete);

/**
 * @namespace Vaadin
 */
window.Vaadin.VcfAutocomplete = VcfAutocomplete;