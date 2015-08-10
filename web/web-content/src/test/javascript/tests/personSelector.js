/*global YAZINO, it, spyOn, FB, expect, describe, jasmine, beforeEach, afterEach, jQuery, strictEqual, $, document, localStorageMock, window, swfobject, alert, clearInterval, clearTimeout, console, setInterval, setTimeout, window, parent, StrataPartnerApi, FB, waitsFor, runs */

describe('Person Selector Widget', function () {

    var samplePeople, callbacks, personSelector, config, preDraw,
        LIST_AVAILABLE = 'available', LIST_SELECTED = 'selected', finalPeople,
        assume = expect, // assume checks assumptions to avoid false positives - not behavior under test
        personA, personB, personC;

    function getPersonElementAt(index, listName) {
        return personSelector.find("." + listName + "People > .person").eq(index);
    }

    function findPersonInList(person, listName) {
        return personSelector.find("." + listName + "People > .person:contains('" + person.displayName + "')");
    }

    function findControl(controlName) {
        return personSelector.find('.' + controlName + 'Control');
    }

    function expectPersonToBeInList(person, listName) {
        expect(findPersonInList(person, listName).length).toBe(1);
    }

    function expectPersonNotToBeInList(person, listName) {
        expect(findPersonInList(person, listName).length).toBe(0);
    }

    function listCount(listName) {
        return personSelector.find('.' + listName + 'People .person').length;
    }

    function setupPersonSelector(testSpecificConfig) {
        var activeConfig = $.extend(true, {}, config, testSpecificConfig || {});
        personSelector = $('<div/>').personSelector(callbacks.continueHandler, activeConfig);
        personSelector.appendTo($('#testArea'));
    }

    function addSamplePeople() {
        personSelector.addPeople(samplePeople);
    }

    beforeEach(function () {
        personSelector = null;
        preDraw = jasmine.createSpy("predraw");
        personA = { id: "101", displayName: "Person A"};
        personB = { id: "102", displayName: "Person B"};
        personC = {id: 103, displayName: "Person C"};
        samplePeople = [personA, personB, personC];
        finalPeople = [];
        callbacks = {
            continueHandler: function (people) {
                finalPeople = people;
            }
        };
        config = {
            hooks: {
                preDrawEvent: preDraw
            }
        };
    });

    it('should exist', function () {
        expect(typeof $('body').personSelector).toBe('function');
    });

    it('initialiser should return elem', function () {
        var rootElem = $('<div/>'),
            returnVal = rootElem.personSelector();

        expect(returnVal).toBe(rootElem);
    });

    it('should throw when given more than one elem', function () {
        var rootElems = $('<div><span>a</span><span>b</span></div>').find('span');

        expect(function () {
            rootElems.personSelector();
        }).toThrow('Requires exactly 1 element.');
    });

    it('should throw when given zero elems', function () {
        var rootElems = $('<div></div>').find('span');

        expect(function () {
            rootElems.personSelector();
        }).toThrow('Requires exactly 1 element.');
    });

    it('should contain continue button', function () {
        setupPersonSelector();

        expect(personSelector.contains('.continueControl')).toBeTruthy();
    });

    it('should contain availablePeople', function () {
        setupPersonSelector();

        expect(personSelector.contains('.availablePeople')).toBeTruthy();
    });

    it('should contain selectedPeople', function () {
        setupPersonSelector();

        expect(personSelector.contains('.selectedPeople')).toBeTruthy();
    });

    it('should contain searchBar', function () {
        setupPersonSelector();

        expect(personSelector.contains('.search input')).toBeTruthy();
    });

    it('should display entries for list of people', function () {
        setupPersonSelector();
        addSamplePeople();

        expect(personSelector.find('.availablePeople > *.person').length).toBe(samplePeople.length);
    });

    it('should show display name for each available person', function () {
        setupPersonSelector();
        addSamplePeople();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonToBeInList(personB, LIST_AVAILABLE);
    });

    it('should show an unselected checkbox for each available person', function () {
        setupPersonSelector();
        addSamplePeople();

        expect(findPersonInList(personA, LIST_AVAILABLE).find("input[type='checkbox']").not('[checked]').length).toBe(1);
    });

    it('should copy the person from available into selected when clicked', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonToBeInList(personA, LIST_SELECTED);
    });

    it('should insert the person at top of the list', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personB, LIST_AVAILABLE).click();
        expect(getPersonElementAt(0, LIST_SELECTED).text()).toContain('Person B');
        findPersonInList(personC, LIST_AVAILABLE).click();

        expect(getPersonElementAt(0, LIST_SELECTED).text()).toContain('Person C');
        expect(getPersonElementAt(1, LIST_SELECTED).text()).toContain('Person B');
    });


    it('should remove the person from selected when clicked in selected', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonToBeInList(personA, LIST_SELECTED);

        findPersonInList(personA, LIST_SELECTED).click();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonNotToBeInList(personA, LIST_SELECTED);
    });

    it('should check the person\'s checkbox from selected when clicked in selected', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();

        expect(findPersonInList(personA, LIST_AVAILABLE).find('input').is(':checked')).toBeTruthy();
        expect(findPersonInList(personA, LIST_SELECTED).find('input').is(':checked')).toBeTruthy();

        findPersonInList(personA, LIST_SELECTED).click();

        expect(findPersonInList(personA, LIST_AVAILABLE).find('input').is(':checked')).toBeFalsy();
        expectPersonNotToBeInList(personA, LIST_SELECTED);
    });

    it('should remove the person from selected when clicked in available', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonToBeInList(personA, LIST_SELECTED);

        findPersonInList(personA, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_AVAILABLE);
        expectPersonNotToBeInList(personA, LIST_SELECTED);
    });

    it('should invoke continue callback when continueControl is activated', function () {
        spyOn(callbacks, 'continueHandler');
        setupPersonSelector();
        addSamplePeople();

        assume(listCount(LIST_AVAILABLE)).toBeGreaterThan(0);
        findPersonInList(personA, LIST_AVAILABLE).click();
        personSelector.find('.continueControl').click();

        expect(callbacks.continueHandler).toHaveBeenCalled();
    });

    it('should pass selected people to contineCallback with continueControl is activated', function () {
        setupPersonSelector();
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();
        findPersonInList(personB, LIST_AVAILABLE).click();
        assume(listCount(LIST_SELECTED)).toBe(2);
        personSelector.find('.continueControl').click();

        expect(finalPeople).toContain(personA);
        expect(finalPeople).toContain(personB);
        expect(finalPeople).not.toContain(personC);

    });

    it('should disable continueControl when no users are selected', function () {
        spyOn(callbacks, 'continueHandler');
        setupPersonSelector();

        var continueControl = personSelector.find('.continueControl');

        expect(continueControl.is('.disabled')).toBeTruthy();
        continueControl.click();
        expect(callbacks.continueHandler).not.toHaveBeenCalled();

    });

    it('should disable continueControl when personSelector is disabled', function () {
        spyOn(callbacks, 'continueHandler');
        setupPersonSelector();
        addSamplePeople();
        findPersonInList(personA, LIST_AVAILABLE).click();

        var continueControl = personSelector.find('.continueControl');

        assume(continueControl.is('.disabled')).toBeFalsy();

        personSelector.disableSelector();

        expect(continueControl.is('.disabled')).toBeTruthy();
        continueControl.click();
        expect(callbacks.continueHandler).not.toHaveBeenCalled();

    });

    it('should re-enable continueControl when personSelector is enabled', function () {
        spyOn(callbacks, 'continueHandler');
        setupPersonSelector();
        addSamplePeople();
        findPersonInList(personA, LIST_AVAILABLE).click();

        var continueControl = personSelector.find('.continueControl');

        assume(continueControl.is('.disabled')).toBeFalsy();

        personSelector.disableSelector();

        assume(continueControl.is('.disabled')).toBeTruthy();

        personSelector.enableSelector();

        expect(continueControl.is('.disabled')).toBeFalsy();
        continueControl.click();
        expect(callbacks.continueHandler).toHaveBeenCalled();

    });

    it('should disable continueControl when no users are selected', function () {
        setupPersonSelector();

        assume(listCount(LIST_SELECTED)).toBe(0);
        expect(findControl('continue').is('.disabled')).toBeTruthy();
    });

    it('should enable continueControl when a user is selected', function () {
        setupPersonSelector();
        addSamplePeople();
        assume(listCount(LIST_SELECTED)).toBe(0);
        assume(findControl('continue').is('.disabled')).toBeTruthy();

        findPersonInList(personA, LIST_AVAILABLE).click();
        assume(listCount(LIST_SELECTED)).toBe(1);

        expect(findControl('continue').is('.disabled')).toBeFalsy();

    });

    it('should disable continueControl when last user is deselected', function () {
        setupPersonSelector();
        addSamplePeople();
        assume(listCount(LIST_SELECTED)).toBe(0);
        assume(findControl('continue').is('.disabled')).toBeTruthy();

        findPersonInList(personA, LIST_AVAILABLE).click();
        findPersonInList(personB, LIST_AVAILABLE).click();
        assume(listCount(LIST_SELECTED)).toBe(2);
        assume(findControl('continue').is('.disabled')).toBeFalsy();
        findPersonInList(personA, LIST_SELECTED).click();
        assume(listCount(LIST_SELECTED)).toBe(1);
        expect(findControl('continue').is('.disabled')).toBeFalsy();
        findPersonInList(personB, LIST_SELECTED).click();
        assume(listCount(LIST_SELECTED)).toBe(0);
        expect(findControl('continue').is('.disabled')).toBeTruthy();

    });

    it('should throw an error if a person lacks a displayName', function () {
        setupPersonSelector();

        expect(function () {
            personSelector.addPeople([
                {id: 1}
            ]);
        }).toThrow('invalid person: missing or empty displayName');

        expect(function () {
            personSelector.addPeople([
                {id: 1, displayName: ""}
            ]);
        }).toThrow('invalid person: missing or empty displayName');

    });

    it('should have css class of personSelector', function () {
        setupPersonSelector();

        expect(personSelector.hasClass('personSelector')).toBeTruthy();

    });

    it('should not add id to dom - keeping the dom clean and avoid surprises from older browsers', function () {
        setupPersonSelector();
        personSelector.addPeople([personA]);

        expect(personSelector.html()).not.toContain(personA.id);

    });

    it('should filter list when filter applied', function () {
        setupPersonSelector();
        addSamplePeople();

        assume(listCount(LIST_AVAILABLE)).toBe(samplePeople.length);
        personSelector.filterByName('non-existent-person');

        expect(listCount(LIST_AVAILABLE)).toBe(0);

    });

    it('should filter list to contain exact match', function () {
        setupPersonSelector();
        addSamplePeople();

        assume(listCount(LIST_AVAILABLE)).toBe(samplePeople.length);
        personSelector.filterByName(personB.displayName);

        expect(listCount(LIST_AVAILABLE)).toBe(1);
        expectPersonToBeInList(personB, LIST_AVAILABLE);

    });

    it('should filter without regard to case', function () {
        setupPersonSelector();
        addSamplePeople();

        assume(listCount(LIST_AVAILABLE)).toBe(samplePeople.length);
        personSelector.filterByName(personB.displayName.toUpperCase());

        expect(listCount(LIST_AVAILABLE)).toBe(1);
        expectPersonToBeInList(personB, LIST_AVAILABLE);

    });

    it('should clear filter when asked', function () {
        setupPersonSelector();
        addSamplePeople();

        assume(listCount(LIST_AVAILABLE)).toBe(samplePeople.length);
        personSelector.filterByName(personB.displayName.toUpperCase());

        assume(listCount(LIST_AVAILABLE)).toBe(1);
        personSelector.clearFilter();

        expect(listCount(LIST_AVAILABLE)).toBe(samplePeople.length);

    });

    it('should filter on multiple space-separated terms', function () {
        setupPersonSelector();
        personSelector.addPeople([
            {
                displayName: 'Matthew Stephen Carey'
            }
        ]);

        assume(listCount(LIST_AVAILABLE)).toBe(1);
        personSelector.filterByName('Mat Carey');

        expect(listCount(LIST_AVAILABLE)).toBe(1);

        personSelector.filterByName('Matthew Carey');

        expect(listCount(LIST_AVAILABLE)).toBe(1);

        personSelector.filterByName('NotMat Carey');

        expect(listCount(LIST_AVAILABLE)).toBe(0);

        personSelector.filterByName('Not');

        expect(listCount(LIST_AVAILABLE)).toBe(0);

        personSelector.filterByName('');

        expect(listCount(LIST_AVAILABLE)).toBe(1);

    });

    it('should update filter when search textfield blurred', function () {
        setupPersonSelector();
        spyOn(personSelector, 'filterByName');

        personSelector.find('.search input').val('a').blur();

        expect(personSelector.filterByName).toHaveBeenCalledWith('a');
    });

    it('should update filter when search textfield changed', function () {
        setupPersonSelector();
        spyOn(personSelector, 'filterByName');

        personSelector.find('.search input').val('a').change();

        expect(personSelector.filterByName).toHaveBeenCalledWith('a');
    });

    it('should update filter on keyup when search focused', function () {
        setupPersonSelector();
        spyOn(personSelector, 'filterByName');

        personSelector.find('.search input').val('a').keyup();

        expect(personSelector.filterByName).toHaveBeenCalledWith('a');
    });

    it('should update filter on keyup when search focused', function () {
        var filterByNameCallCount = 0;
        setupPersonSelector();
        personSelector.filterByName = function () {
            filterByNameCallCount += 1;
        };

        personSelector.find('.search input').val('a').keyup().change().blur();

        expect(filterByNameCallCount).toBe(1);
    });

    it('should clear filter when search field is blank', function () {
        setupPersonSelector();
        spyOn(personSelector, 'filterByName');
        spyOn(personSelector, 'clearFilter');

        personSelector.find('.search input').val('a').val('').change();

        expect(personSelector.filterByName).not.toHaveBeenCalled();
        expect(personSelector.clearFilter).toHaveBeenCalled();
    });

    it('should clear filter when search field contains only whitespace', function () {
        setupPersonSelector();
        spyOn(personSelector, 'filterByName');
        spyOn(personSelector, 'clearFilter');

        personSelector.find('.search input').val('a').val(' \t\n\r').change();

        expect(personSelector.filterByName).not.toHaveBeenCalled();
        expect(personSelector.clearFilter).toHaveBeenCalled();
    });


    it('should sort people alphabetically, ascending by display name', function () {
        setupPersonSelector();
        personSelector.addPeople([
            {displayName: 'charles'},
            {displayName: 'alfred'},
            {displayName: 'gimp'}
        ]);

        assume(listCount(LIST_AVAILABLE)).toBe(3);

        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toContain('alfred');
        expect(getPersonElementAt(1, LIST_AVAILABLE).text()).toContain('charles');
        expect(getPersonElementAt(2, LIST_AVAILABLE).text()).toContain('gimp');

    });


    it('should sort filtered people alphabetically, ascending by display name', function () {
        setupPersonSelector();
        personSelector.addPeople([
            {displayName: 'charles included'},
            {displayName: 'alfred included'},
            {displayName: 'gimp excluded'}
        ]);

        assume(listCount(LIST_AVAILABLE)).toBe(3);
        personSelector.filterByName('included');

        assume(listCount(LIST_AVAILABLE)).toBe(2);
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toContain('alfred');
        expect(getPersonElementAt(1, LIST_AVAILABLE).text()).toContain('charles');

    });


    it('should sort people alphabetically, paging them alphabetically', function () {
        setupPersonSelector({pageSize: 3});
        personSelector.addPeople([
            {displayName: 'charles'},
            {displayName: 'alfred'},
            {displayName: 'zappa'},
            {displayName: 'frank'},
            {displayName: 'mcdonald'},
            {displayName: 'kermit'},
            {displayName: 'ronald'}
        ]);

        assume(listCount(LIST_AVAILABLE)).toBe(3);

        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toContain('alfred');
        expect(getPersonElementAt(1, LIST_AVAILABLE).text()).toContain('charles');
        expect(getPersonElementAt(2, LIST_AVAILABLE).text()).toContain('frank');
        personSelector.nextPage();
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toContain('kermit');
        expect(getPersonElementAt(1, LIST_AVAILABLE).text()).toContain('mcdonald');
        expect(getPersonElementAt(2, LIST_AVAILABLE).text()).toContain('ronald');
        personSelector.nextPage();
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toContain('zappa');

    });

    it("should call pre draw hook on ModelChanged with array of people", function () {
        setupPersonSelector({pageSize: 2});
        addSamplePeople();
        personSelector.modelChanged();
        expect(preDraw).toHaveBeenCalledWith([personA, personB]);
    });

    it("shouldShowFirstPaginatedPage", function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();

        expect(personSelector.find('.availablePeople > .person').length).toBe(1);
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person A');
    });

    it("shouldShowSecondPaginatedPage", function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();
        personSelector.nextPage();
        expect(personSelector.find('.availablePeople > .person').length).toBe(1);
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person B');
    });

    it("shouldChangeToPreviousPage", function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();
        personSelector.nextPage();
        personSelector.prevPage();
        expect(personSelector.find('.availablePeople > .person').length).toBe(1);
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person A');
    });

    it("shouldNotChangeToPreviousPageIfOnFirstPage", function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();
        personSelector.prevPage();
        expect(personSelector.find('.availablePeople > .person').length).toBe(1);
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person A');
    });

    it("shouldNotChangeToNextPageIfOnLastPage", function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();
        personSelector.nextPage();
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person B');
        personSelector.nextPage();
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person C');
        personSelector.nextPage();
        expect(getPersonElementAt(0, LIST_AVAILABLE).text()).toBe('Person C');
    });

    it('should pass selected correct people to continueCallback when people are selected on multiple pages', function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();

        getPersonElementAt(0, LIST_AVAILABLE).click();

        personSelector.nextPage();
        getPersonElementAt(0, LIST_AVAILABLE).click();
        assume(listCount(LIST_SELECTED)).toBe(2);
        personSelector.find('.continueControl').click();

        expect(finalPeople).toContain(personA);
        expect(finalPeople).toContain(personB);
        assume(finalPeople).not.toContain(personC);

    });

    it('should allow setting up disabled people', function () {
        setupPersonSelector();
        personB.disabled = true;
        addSamplePeople();

        assume(getPersonElementAt(0, LIST_AVAILABLE).is('.disabled')).toBeFalsy();
        expect(getPersonElementAt(1, LIST_AVAILABLE).is('.disabled')).toBeTruthy();
        assume(getPersonElementAt(2, LIST_AVAILABLE).is('.disabled')).toBeFalsy();

    });

    //set disabled state for next/prev when invalid

    it('should remove prev button when first page', function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();

        expect(personSelector.find('.nextPageButton').is(":visible")).toBeTruthy();
        expect(personSelector.find('.previousPageButton').is(":visible")).toBeFalsy();
    });

    it('should remove next button when last page', function () {
        setupPersonSelector({pageSize: 3});
        addSamplePeople();

        expect(personSelector.find('.nextPageButton').is(":visible")).toBeFalsy();
        expect(personSelector.find('.previousPageButton').is(":visible")).toBeFalsy();
    });

    it('should enable both buttons when on middle page of 3', function () {
        setupPersonSelector({pageSize: 1});
        addSamplePeople();
        personSelector.nextPage();

        expect(personSelector.find('.nextPageButton').is(":visible")).toBeTruthy();
        expect(personSelector.find('.previousPageButton').is(":visible")).toBeTruthy();
    });

    it('should allow setting changing disabled status after loading people', function () {
        setupPersonSelector();
        personB.disabled = true;
        addSamplePeople();

        assume(getPersonElementAt(0, LIST_AVAILABLE).is('.disabled')).toBeFalsy();
        assume(getPersonElementAt(1, LIST_AVAILABLE).is('.disabled')).toBeTruthy();
        assume(getPersonElementAt(2, LIST_AVAILABLE).is('.disabled')).toBeFalsy();

        personB.disabled = false;
        personSelector.modelChanged();

        assume(getPersonElementAt(0, LIST_AVAILABLE).is('.disabled')).toBeFalsy();
        expect(getPersonElementAt(1, LIST_AVAILABLE).is('.disabled')).toBeFalsy();
        assume(getPersonElementAt(2, LIST_AVAILABLE).is('.disabled')).toBeFalsy();

    });

    it('should include comments related to people', function () {
        setupPersonSelector();
        personB.comment = "I am person B";
        personC.comment = "I am person C";
        addSamplePeople();

        assume(getPersonElementAt(0, LIST_AVAILABLE).contains('.comment')).toBeFalsy();
        expect(getPersonElementAt(1, LIST_AVAILABLE).find('.comment').text()).toBe("I am person B");
        expect(getPersonElementAt(2, LIST_AVAILABLE).find('.comment').text()).toBe("I am person C");

    });

    it('should not allow selecting disabled people', function () {
        var currentPersonIndex = samplePeople.length;
        setupPersonSelector();
        personA.disabled = false;
        personB.disabled = true;
        personC.disabled = false;
        addSamplePeople();

        while (currentPersonIndex) {
            currentPersonIndex -= 1;
            getPersonElementAt(currentPersonIndex, LIST_AVAILABLE).click();
        }

        assume(listCount(LIST_SELECTED)).toBe(2);
        personSelector.find('.continueControl').click();

        expect(finalPeople).toContain(personA);
        expect(finalPeople).not.toContain(personB);
        assume(finalPeople).toContain(personC);

    });

    it('should contain selected people title', function () {
        var labelText = 'this is the label.';
        setupPersonSelector({
            copy: {
                peopleSelectedLabel: labelText
            }
        });
        addSamplePeople();

        expect(personSelector.text()).toContain(labelText);

    });

    it('should be able to add less than the maximum number of people', function () {
        var maxHitAction  = jasmine.createSpy("maxHitAction");
        setupPersonSelector({maxSelectable: 5, maxSendLimitHitAction: maxHitAction});
        addSamplePeople();

        findPersonInList(personA, LIST_AVAILABLE).click();
        findPersonInList(personB, LIST_AVAILABLE).click();
        findPersonInList(personC, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_SELECTED);
        expectPersonToBeInList(personB, LIST_SELECTED);
        expectPersonToBeInList(personC, LIST_SELECTED);

        assume(listCount(LIST_SELECTED)).toBe(3);
        expect(maxHitAction).not.toHaveBeenCalled();
    });

    it('shouldn\'t be able to invite more than configured amount of people (50 for FB)', function () {
        var maxHitAction  = jasmine.createSpy("maxHitAction");
        setupPersonSelector({maxSelectable: 1, maxSendLimitHitAction: maxHitAction});
        addSamplePeople();

        expectPersonNotToBeInList(personA, LIST_SELECTED);
        expectPersonNotToBeInList(personB, LIST_SELECTED);

        findPersonInList(personA, LIST_AVAILABLE).click();
        findPersonInList(personB, LIST_AVAILABLE).click();

        expectPersonToBeInList(personA, LIST_SELECTED);
        expectPersonNotToBeInList(personB, LIST_SELECTED);
        assume(listCount(LIST_SELECTED)).toBe(1);
        expect(maxHitAction).toHaveBeenCalled();
    });

});
